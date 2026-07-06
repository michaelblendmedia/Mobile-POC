package com.example.sfmcregister.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Penyimpanan persisten Contact Key (UUID) via SharedPreferences.
 *
 * Contact Key disimpan agar STABIL — user tidak dibuatkan Contact Key baru
 * setiap membuka app, sehingga tidak terjadi duplikasi contact di MC/Data Cloud.
 */
@Singleton
class ContactPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var contactKey: String?
        get() = prefs.getString(KEY_CONTACT, null)
        set(value) = prefs.edit { putString(KEY_CONTACT, value) }

    val isRegistered: Boolean get() = !contactKey.isNullOrBlank()

    /**
     * Mode manual: SDK TIDAK di-init saat app dibuka; init + registrasi baru
     * terjadi saat tombol Register ditekan. Berlaku penuh setelah app restart.
     */
    var manualInitMode: Boolean
        get() = prefs.getBoolean(KEY_MANUAL_INIT, false)
        set(value) = prefs.edit { putBoolean(KEY_MANUAL_INIT, value) }

    var firstName: String?
        get() = prefs.getString(KEY_FIRST_NAME, "Pelanggan")
        set(value) = prefs.edit { putString(KEY_FIRST_NAME, value) }

    companion object {
        private const val PREF_NAME = "sfmc_contact_prefs"
        private const val KEY_CONTACT = "contact_key"
        private const val KEY_MANUAL_INIT = "manual_init_mode"
        private const val KEY_FIRST_NAME = "first_name"
    }
}
