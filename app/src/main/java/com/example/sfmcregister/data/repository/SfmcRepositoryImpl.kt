package com.example.sfmcregister.data.repository

import android.util.Log
import com.example.sfmcregister.data.local.ContactPreferences
import com.example.sfmcregister.data.model.RegisterForm
import com.example.sfmcregister.data.sfmc.SfmcInitializer
import com.example.sfmcregister.util.SfmcResult
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Inti integrasi Marketing Cloud.
 *
 * Alur:
 *  1. Generate/reuse Contact Key (UUID.randomUUID()).
 *  2. Simpan Contact Key ke SharedPreferences.
 *  3. Set Contact Key via identity.setProfileId (API resmi, non-deprecated).
 *  4. Set profile attributes via identity.setProfileAttributes (batch).
 *
 * Catatan: setProfileId + setProfileAttributes OTOMATIS mengantre & mengirim
 * registrasi ke server MC. Tidak perlu (dan tidak ada) tombol "sync" manual —
 * inilah best practice SFMC SDK modern. Data lalu mengalir ke Data Cloud via
 * connector MC↔Data Cloud (server-side) sesuai mapping ke Individual DMO.
 */
class SfmcRepositoryImpl @Inject constructor(
    private val prefs: ContactPreferences,
    private val initializer: SfmcInitializer
) : SfmcRepository {

    override suspend fun registerContact(form: RegisterForm): SfmcResult<String> {
        return try {
            // Mode AUTO: init sudah jalan sejak app dibuka → langsung lolos.
            // Mode MANUAL: init SDK baru DIMULAI di sini (saat Register ditekan).
            initializer.ensureInitialized(
                if (prefs.manualInitMode) "MANUAL (tombol Register)" else "AUTO (sudah init)"
            )

            // 1. Contact Key: prioritas → (a) override dari form bila diisi,
            //    (b) yang tersimpan (stabil), (c) UUID baru.
            //    (Butuh identitas baru? Pakai tombol "Reset Device ID" di app.)
            val contactKey = form.contactKeyOverride.trim().ifEmpty { null }
                ?: prefs.contactKey
                ?: UUID.randomUUID().toString()

            // 2. Simpan ke SharedPreferences
            prefs.contactKey = contactKey

            // 3 & 4. Kirim ke MC via Identity module (auto-sync).
            // Sesuai docs "Contact Registration" (Mobile Unified SDK):
            // party identifier = identitas unik individu di Marketing Cloud.
            // Registrasi dianggap invalid bila field wajib tidak lengkap.
            val sdk = awaitSdkReady()
            sdk.identity.edit {
                profileId = contactKey
                // Party identifier — Number = nilai uniknya (UUID kita),
                // Name/Type harus konsisten dengan mapping Party Identification
                // di Data Cloud (samakan dengan konfigurasi identity resolution).
                partyIdentificationNumber = contactKey
                partyIdentificationName = "SimplePocContact"
                partyIdentificationType = "CustomerId"
                val attrs = mutableMapOf<String, String>()
                attrs["firstName"] = form.firstName.trim()
                attrs["lastName"] = form.lastName.trim()
                attrs["email"] = form.email.trim()

                // Tambahkan field custom jika tidak kosong
                if (form.age.isNotBlank()) attrs["age"] = form.age.trim()
                if (form.birthDate.isNotBlank()) attrs["birthDate"] = form.birthDate.trim()
                if (form.city.isNotBlank()) attrs["city"] = form.city.trim()
                if (form.gender.isNotBlank()) attrs["gender"] = form.gender.trim()
                if (form.occupation.isNotBlank()) attrs["occupation"] = form.occupation.trim()
                if (form.phone.isNotBlank()) attrs["phone"] = form.phone.trim()
                if (form.province.isNotBlank()) attrs["province"] = form.province.trim()

                attributes.putAll(attrs)
            }

            // Fire an explicit custom event for registration
            trackEvent("User_Registered", mapOf("ContactKey" to contactKey))

            Log.i(TAG, "Contact registered: $contactKey")
            SfmcResult.Success(contactKey)
        } catch (t: Throwable) {
            Log.e(TAG, "registerContact failed", t)
            SfmcResult.Error("Gagal mendaftarkan contact: ${t.message}", t)
        }
    }

    override suspend fun trackEvent(name: String, attributes: Map<String, Any>) {
        try {
            val sdk = awaitSdkReady()
            // Menggunakan EventManager dari SFMCSdk untuk Event Tracking
            val event = com.salesforce.marketingcloud.sfmcsdk.components.events.EventManager.customEvent(name, attributes)
            SFMCSdk.track(event)
            Log.i(TAG, "Event tracked: $name")
        } catch (t: Throwable) {
            Log.e(TAG, "trackEvent failed for $name", t)
        }
    }

    /** Bungkus SFMCSdk.requestSdk (callback) menjadi suspend function. */
    private suspend fun awaitSdkReady(): SFMCSdk =
        suspendCancellableCoroutine { cont ->
            SFMCSdk.requestSdk { sdk ->
                if (cont.isActive) cont.resume(sdk)
            }
        }

    companion object {
        private const val TAG = "SFMC"
    }
}
