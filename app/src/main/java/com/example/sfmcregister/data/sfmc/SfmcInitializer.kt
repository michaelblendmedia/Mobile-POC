package com.example.sfmcregister.data.sfmc

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.sfmcregister.BuildConfig
import com.example.sfmcregister.data.local.ContactPreferences
import com.example.sfmcregister.MainActivity
import com.example.sfmcregister.R
import com.salesforce.marketingcloud.UrlHandler
import com.salesforce.marketingcloud.inappmessagingfeature.config.InAppMessagingFeatureConfig
import com.salesforce.marketingcloud.mobileappmessaging.MobileAppMessagingConfig
import com.salesforce.marketingcloud.pushfeature.config.PushFeatureConfig
import com.salesforce.marketingcloud.pushfeature.notifications.NotificationCustomizationOptions
import com.salesforce.marketingcloud.pushfeature.notifications.NotificationManager
import java.util.Random
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdkModuleConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess

/**
 * Satu-satunya pintu inisialisasi SFMC SDK, mendukung dua mode:
 *
 *  - AUTO   : dipanggil dari Application.onCreate (perilaku standar/rekomendasi).
 *  - MANUAL : dipanggil dari flow Register — TIDAK ADA komunikasi ke Salesforce
 *             sebelum user menekan tombol Register.
 *
 * Init hanya terjadi sekali per proses (guard AtomicBoolean); pemanggil kedua
 * cukup menunggu hasil init pertama.
 */
@Singleton
class SfmcInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: ContactPreferences
) {
    private val started = AtomicBoolean(false)
    private val initDone = CompletableDeferred<String>()

    /** Mulai init bila belum pernah. Aman dipanggil berkali-kali. */
    fun initializeAsync(source: String) {
        if (!started.compareAndSet(false, true)) return

        Log.i(TAG, "SFMC SDK init dimulai — sumber: $source")

        // Channel khusus untuk notifikasi ber-URL — WAJIB benar-benar dibuat;
        // notif yang di-assign ke channel yang tidak ada TIDAK akan tampil.
        createUrlNotificationChannel()

        // Handler bersama untuk aksi URL (tombol IAM, button/carousel push)
        val urlHandler = UrlHandler { ctx, url, _ ->
            PendingIntent.getActivity(
                ctx,
                Random().nextInt(),
                Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        SFMCSdk.configure(
            context.applicationContext as Application,
            SFMCSdkModuleConfig.build {
                // Marketing Cloud ADVANCED — modul MAM (Mobile App Messaging)
                mamModuleConfig = MobileAppMessagingConfig.builder().apply {
                    moduleApplicationId(BuildConfig.MAM_APP_ID)
                    accessToken(BuildConfig.MAM_ACCESS_TOKEN)
                    tenantId(BuildConfig.MAM_TENANT_ID)
                    endpointUrl(BuildConfig.MAM_ENDPOINT_URL)
                    analyticsEnabled(true)
                }.build()

                // Modul push (FCM) — hanya bila sender id terisi.
                // Kustomisasi notifikasi (Basic Customization, sesuai docs):
                //  - icon status bar
                //  - tap notif  : buka MainActivity, atau buka URL bila notif membawa URL
                //  - channel    : default channel SDK ("marketing"), channel khusus utk notif ber-URL
                // In-App Messaging — pesan tampil DI DALAM app (tanpa FCM/push).
                // SDK otomatis fetch pesan dari server; UrlHandler menangani
                // aksi tombol yang membawa URL.
                inAppMessagingFeatureModuleConfig = InAppMessagingFeatureConfig.builder().apply {
                    setUrlHandler(urlHandler)
                }.build()

                if (BuildConfig.FCM_SENDER_ID.isNotBlank()) {
                    pushFeatureModuleConfig = PushFeatureConfig.builder().apply {
                        setSenderId(BuildConfig.FCM_SENDER_ID)
                        setUrlHandler(urlHandler)
                        setNotificationCustomizationOptions(
                            NotificationCustomizationOptions.create(
                                R.drawable.ic_notification,
                                NotificationManager.NotificationLaunchIntentProvider { ctx, message ->
                                    val requestCode = Random().nextInt()
                                    val url = message.url
                                    if (url.isNullOrEmpty()) {
                                        // Tanpa URL → buka app (MainActivity)
                                        PendingIntent.getActivity(
                                            ctx,
                                            requestCode,
                                            Intent(ctx, MainActivity::class.java),
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        )
                                    } else {
                                        // Ada URL → buka browser/deep link
                                        PendingIntent.getActivity(
                                            ctx,
                                            requestCode,
                                            Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        )
                                    }
                                },
                                NotificationManager.NotificationChannelIdProvider { ctx, message ->
                                    if (message.url.isNullOrEmpty()) {
                                        // Channel default SDK ("marketing")
                                        NotificationManager.createDefaultNotificationChannel(ctx)
                                    } else {
                                        URL_CHANNEL_ID
                                    }
                                }
                            )
                        )
                    }.build()
                }
            }
        ) { initStatus ->
            Log.i(TAG, "SFMC SDK init status: ${initStatus.status} (sumber: $source)")
            initDone.complete(initStatus.status.toString())
        }
    }

    /**
     * Pastikan SDK ter-init lalu tunggu selesai.
     * Mode AUTO: init sudah jalan → langsung lolos.
     * Mode MANUAL: init baru dimulai DI SINI (saat Register ditekan).
     */
    suspend fun ensureInitialized(source: String) {
        initializeAsync(source)
        initDone.await()
    }

    /** Buat notification channel untuk notif yang membawa URL (Android 8+). */
    private fun createUrlNotificationChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager
        manager.createNotificationChannel(
            android.app.NotificationChannel(
                URL_CHANNEL_ID,
                "Marketing (dengan tautan)",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    /**
     * TESTING: WIPE TOTAL data app (setara Settings → Clear Storage).
     *
     * Menghapus selektif tidak cukup — device/registration ID SDK tersimpan di
     * storage terenkripsi yang namanya tidak selalu cocok filter, dan ada race
     * saat proses ditutup sehingga ID lama ditulis ulang. clearApplicationUserData()
     * menghapus SEMUA (prefs, database, files, cache) lalu mematikan proses,
     * sehingga saat app dibuka lagi SDK membuat Device ID DAN Party BENAR-BENAR baru.
     */
    fun resetDeviceIdentityAndExit() {
        Log.w(TAG, "RESET: wipe total data app (device id + party baru)...")

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val wiped = am.clearApplicationUserData()
        Log.w(TAG, "clearApplicationUserData() = $wiped")

        // Fallback bila clearApplicationUserData gagal (jarang): hapus manual + exit.
        if (!wiped) {
            prefs.contactKey = null
            File(context.applicationInfo.dataDir, "shared_prefs").listFiles()
                ?.forEach { context.deleteSharedPreferences(it.nameWithoutExtension) }
            context.databaseList().forEach { context.deleteDatabase(it) }
            exitProcess(0)
        }
        // Bila berhasil, sistem otomatis menghentikan proses app.
    }

    companion object {
        private const val TAG = "SFMC"
        private const val URL_CHANNEL_ID = "UrlNotification"
    }
}
