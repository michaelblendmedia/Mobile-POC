package com.example.sfmcregister

import android.app.Application
import android.util.Log
import com.example.sfmcregister.data.local.ContactPreferences
import com.example.sfmcregister.data.sfmc.SfmcInitializer
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogLevel
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogListener
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class.
 *
 * Mode AUTO (default): SFMC SDK di-init di sini (rekomendasi resmi Salesforce).
 * Mode MANUAL (untuk testing): init DITUNDA — tidak ada komunikasi apa pun ke
 * Salesforce sampai user menekan tombol Register (lihat SfmcInitializer).
 */
@HiltAndroidApp
class SfmcRegisterApp : Application() {

    @Inject lateinit var prefs: ContactPreferences
    @Inject lateinit var sfmcInitializer: SfmcInitializer

    override fun onCreate() {
        super.onCreate()

        // Logging verbose HANYA saat debug — untuk verifikasi registrasi.
        if (BuildConfig.DEBUG) {
            SFMCSdk.setLogging(LogLevel.DEBUG, LogListener.AndroidLogger())
        }

        if (prefs.manualInitMode) {
            Log.i(TAG, "Mode MANUAL aktif — SFMC SDK TIDAK di-init saat startup; menunggu tombol Register.")
        } else {
            sfmcInitializer.initializeAsync("AUTO (Application.onCreate)")
        }
    }

    companion object {
        private const val TAG = "SFMC"
    }
}
