// ✅ UPDATED build.gradle.kts for app module
// This file should replace app/build.gradle.kts
// Key changes:
// 1. Added buildConfigField for API_BASE_URL in debug/release builds
// 2. Added manifestPlaceholders for usesCleartextTraffic
// 3. Now you can change BASE_URL without rebuilding for different environments

plugins {

    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.jsac.sync"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jsac.sync"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false

            // ✅ FIXED (FIX #2): Set API URL for debug builds
            // Change YOUR_IP to match where your Flask backend is running:
            // - "http://192.168.87.80:5000/" (for physical machine on network)
            // - "http://10.0.2.2:5000/" (for Android Emulator on same machine)
            // - "http://localhost:5000/" (for same machine, localhost)
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:5000/\"")

            // Allow cleartext (HTTP) traffic for development only
            manifestPlaceholders["usesCleartextTraffic"] = true
        }

        release {
            isMinifyEnabled = false

            // ✅ FIXED (FIX #2): Set API URL for release builds
            // IMPORTANT: Use HTTPS and your actual domain in production!
            buildConfigField("String", "API_BASE_URL", "\"https://api.yourdomain.com/\"")

            // Don't allow cleartext traffic in production
            manifestPlaceholders["usesCleartextTraffic"] = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true  // ✅ Enable BuildConfig generation
    }
}

dependencies {
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Activity
    implementation("androidx.activity:activity-ktx:1.8.2")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Gson (for parsing JSON)
    implementation("com.google.code.gson:gson:2.10.1")
    // Fragment
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Logging
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Gson Converter
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Logging Interceptor
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Testing - JUnit
    testImplementation("junit:junit:4.13.2")

    // Android Test - Instrumented Tests
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}