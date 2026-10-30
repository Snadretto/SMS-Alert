plugins { id("com.android.application") version "9.4.0" }
android {
    namespace = "ch.snadretto.smsalert.probe"
    compileSdk = 37
    defaultConfig { applicationId = "ch.snadretto.smsalert.probe"; minSdk = 31; targetSdk = 37; versionCode = 1; versionName = "1" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
