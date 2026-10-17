plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.offlineflow.tamil"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.offlineflow.tamil"
        minSdk = 28
        targetSdk = 35
        versionCode = 8
        versionName = "0.8.0"
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    ndkVersion = "26.3.11579264"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // Deliberately mandatory: a "successful" APK must contain the native
    // speech engine. Without whisper.cpp source the build fails closed.
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}
