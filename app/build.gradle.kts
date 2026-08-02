plugins {
    id("com.android.application")
}

android {
    namespace = "com.watchpee"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.watchpee"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
