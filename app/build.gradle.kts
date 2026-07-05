import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

// Baca kredensial dari local.properties (tidak di-hardcode di source)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun prop(key: String): String = (localProps[key] as String?) ?: ""

android {
    namespace = "com.example.sfmcregister"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.sfmcregister"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Kredensial Marketing Cloud ADVANCED (modul MAM) → BuildConfig
        buildConfigField("String", "MAM_APP_ID", "\"${prop("MAM_APP_ID")}\"")
        buildConfigField("String", "MAM_ACCESS_TOKEN", "\"${prop("MAM_ACCESS_TOKEN")}\"")
        buildConfigField("String", "MAM_TENANT_ID", "\"${prop("MAM_TENANT_ID")}\"")
        buildConfigField("String", "MAM_ENDPOINT_URL", "\"${prop("MAM_ENDPOINT_URL")}\"")
        // Firebase sender id (push token)
        buildConfigField("String", "FCM_SENDER_ID", "\"${prop("FCM_SENDER_ID")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // --- Salesforce Marketing Cloud Unified Mobile SDK ---
    // Repo maven ada di settings.gradle.kts (github.io/.../repository)
    implementation("com.salesforce.marketingcloud:marketingcloudsdk:11.0.0")
    // Modul MAM (Mobile App Messaging) — WAJIB untuk Marketing Cloud Advanced
    // (endpoint umamobile). Sesuai LearningApp resmi Salesforce.
    implementation("com.salesforce.marketingcloud:mobileappmessagingsdk:1.1.+")

    // Firebase Cloud Messaging — untuk push token (deviceSystemToken di registrasi)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")

    // --- Compose ---
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- Hilt ---
    implementation("com.google.dagger:hilt-android:2.57.2")
    ksp("com.google.dagger:hilt-compiler:2.57.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // --- Core ---
    implementation("androidx.core:core-ktx:1.15.0")
}
