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

            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"http://192.168.231.80:5000/\""
            )

            manifestPlaceholders["usesCleartextTraffic"] = true
        }

        release {
            isMinifyEnabled = false

            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://api.yourdomain.com/\""
            )

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
        buildConfig = true
    }
}

dependencies {
    // ============================================
    // CORE ANDROID
    // ============================================
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ============================================
    // LIFECYCLE & COROUTINES
    // ============================================
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ============================================
    // DATABASE - ROOM
    // ============================================
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ============================================
    // DATA STORAGE
    // ============================================
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ============================================
    // NETWORKING - RETROFIT & HTTP
    // ============================================
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ============================================
    // JSON
    // ============================================
    implementation("com.google.code.gson:gson:2.10.1")

    // ============================================
    // DEPENDENCY INJECTION - HILT
    // ============================================
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")

    // ✅ REQUIRED: Hilt for WorkManager
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // ============================================
    // BACKGROUND WORK - WORKMANAGER
    // ============================================
    // ✅ REQUIRED: WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // ============================================
    // LOCATION SERVICES
    // ============================================
    // ✅ REQUIRED: Google Play Services for Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // ============================================
    // NAVIGATION
    // ============================================
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // ============================================
    // TESTING
    // ============================================
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}