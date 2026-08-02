plugins {
    id("com.android.application")
}

android {
    namespace = "com.intermission"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.intermission"
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
