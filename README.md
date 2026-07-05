# SFMC Register — Android (Kotlin + Compose)

Aplikasi Android native yang mendaftarkan user (First Name, Last Name, Email) ke
**Salesforce Marketing Cloud** via **SFMC Advanced Core SDK**. Contact Key
(UUID) digenerate otomatis dan disimpan di SharedPreferences. Data mengalir ke
**Data Cloud** melalui connector MC↔Data Cloud (server-side).

## Teknologi
Kotlin · MVVM · Hilt · Coroutines · Jetpack Compose · Material 3 · Marketing Cloud Unified Mobile SDK 11.0.0 (engagementModuleConfig)

## Setup

1. **Isi kredensial** — salin `local.properties.example` menjadi `local.properties`
   (file ini di-gitignore, tidak ikut ter-commit) lalu isi:
   ```
   MAM_APP_ID=...
   MAM_ACCESS_TOKEN=...
   MAM_TENANT_ID=...
   MAM_ENDPOINT_URL=https://{tenant-id}.c360a.salesforce.com/
   FCM_SENDER_ID=...
   ```
   Nilai MAM diambil dari halaman setup **Mobile App** di Marketing Cloud
   **Advanced** (modul MAM / Mobile App Messaging — bukan MC Engagement).
   Endpoint WAJIB diakhiri slash.

2. **Firebase** — `app/google-services.json` diperlukan (plugin google-services
   aktif). File ini di-gitignore, jadi unduh sendiri dari Firebase Console →
   Project Settings → Your apps. `FCM_SENDER_ID` = `project_number` di file tersebut.

3. **Buka di Android Studio** (Ladybug+), lalu **Sync Gradle** & Run.

   Atau via CLI (butuh Gradle 8.11+ terpasang bila wrapper belum ada):
   ```
   gradle wrapper        # generate ./gradlew sekali saja
   ./gradlew assembleDebug
   ```

## Struktur

| File | Peran |
|------|-------|
| `SfmcRegisterApp.kt` | Init SFMC SDK di Application.onCreate (sekali) |
| `data/local/ContactPreferences.kt` | Simpan Contact Key (UUID) persisten |
| `data/repository/SfmcRepositoryImpl.kt` | UUID → setProfileId + setProfileAttributes |
| `ui/register/*` | Form Compose + ViewModel + UiState |
| `ui/success/SuccessScreen.kt` | Layar sukses |
| `ui/navigation/AppNavigation.kt` | Navigasi register → success |
| `di/AppModule.kt` | Hilt bindings |

## Verifikasi data masuk

1. **App (Logcat)** — filter tag `SFMC`; cari log `Contact registered: <uuid>` dan
   HTTP 200 ke endpoint `.../registration/`.
2. **Marketing Cloud** — Contact Builder → All Contacts → cari Contact Key (UUID);
   pastikan FirstName/LastName/Email terisi.
3. **Data Cloud** — Data Streams (connector MobilePush) status Active & Last Run
   terbaru → Data Explorer / Individual DMO menampilkan record.

> Jika data tidak sampai ke DMO: cek **nama attribute** (`firstName`/`lastName`/
> `email`) harus PERSIS cocok dengan mapping (case-sensitive), dan Data Stream
> sudah di-refresh.

## Catatan API
Menggunakan `SFMCSdk.configure`, `SFMCSdk.requestSdk`, `identity.setProfileId`,
`identity.setProfileAttributes` — API resmi & non-deprecated pada SFMC SDK 8.x.
Verifikasi versi terbaru di:
https://salesforce-marketingcloud.github.io/MarketingCloudSDK-Android/
