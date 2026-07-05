package com.example.sfmcregister

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.sfmcregister.ui.navigation.AppNavigation
import com.example.sfmcregister.ui.theme.SfmcRegisterTheme
import com.salesforce.marketingcloud.pushfeature.PushFeature
import dagger.hilt.android.AndroidEntryPoint

/**
 * Satu-satunya Activity (single-activity architecture).
 * Menampilkan Compose UI, mengatur navigasi, dan meminta izin notifikasi
 * (Android 13+ mewajibkan runtime permission POST_NOTIFICATIONS —
 * tanpa ini registrasi terkirim dengan pushEnabled=false dan notif tak tampil).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.i("SFMC", "Izin notifikasi ${if (granted) "DIBERIKAN ✅" else "DITOLAK ❌"}")
            if (granted) enableSdkPush()
        }

    /** Aktifkan push di level SDK + log statusnya (menentukan isPushEnabled
     *  pada payload registrasi). */
    private fun enableSdkPush() {
        PushFeature.requestSdk { pf ->
            pf.getPushMessageManager().enablePush()
            Log.i(
                "SFMC",
                "SDK pushEnabled = ${pf.getPushMessageManager().isPushEnabled()}"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        setContent {
            SfmcRegisterTheme {
                AppNavigation()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                enableSdkPush()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // < Android 13: tidak butuh runtime permission
            enableSdkPush()
        }
    }
}
