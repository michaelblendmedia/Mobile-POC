package com.example.sfmcregister.data.repository

import com.example.sfmcregister.data.model.RegisterForm
import com.example.sfmcregister.util.SfmcResult

/** Kontrak operasi Marketing Cloud. */
interface SfmcRepository {

    /**
     * Generate/reuse Contact Key (UUID), simpan ke SharedPreferences, lalu
     * daftarkan Contact Key + profile attributes ke Marketing Cloud.
     *
     * @return Contact Key (UUID) bila sukses.
     */
    suspend fun registerContact(form: RegisterForm): SfmcResult<String>

    /**
     * Melacak event khusus (Custom Event) ke Marketing Cloud.
     * Fitur Event Tracking (MAM SDK).
     */
    suspend fun trackEvent(name: String, attributes: Map<String, Any> = emptyMap())

    /**
     * Kirim Custom Event secara langsung (tanpa antrean batch) via
     * SFMCSdk.sendImmediate — dipakai untuk trigger In-App Message
     * yang butuh evaluasi real-time (mis. transfer_page_open).
     */
    suspend fun sendEventImmediate(name: String, attributes: Map<String, Any> = emptyMap())
}
