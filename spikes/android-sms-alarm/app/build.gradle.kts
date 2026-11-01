plugins {
    id("com.android.application")
}

android {
    namespace = "ch.snadretto.smsalert.spike"
    compileSdk = 37

    defaultConfig {
        applicationId = "ch.snadretto.smsalert.spike"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "0.1-spike"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
