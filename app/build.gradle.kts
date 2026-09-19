plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "hidden.the.projectx"
    compileSdk = 34

    defaultConfig {
        applicationId = "hidden.the.projectx"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // Mengambil variabel lingkungan MAPS_API_KEY.
        // Diberikan fallback string jika environment variable tidak ditemukan.
        val mapsApiKey = System.getenv("MAPS_API_KEY") ?: "DEFAULT_FALLBACK_KEY"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    signingConfigs {
        create("release") {
            // Memastikan jalur lokasi file keystore mengarah ke direktori 'app/'
            storeFile = file("aya.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Xposed API: compileOnly agar tidak di-bundle ke dalam APK
    compileOnly("de.robv.android.xposed:api:82")
}
