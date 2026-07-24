plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.tomasloksa.cgeowear.bridge"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.tomasloksa.cgeowear"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {
    implementation(project(":common"))
    implementation(libs.play.services.wearable)
    implementation(libs.play.services.location)
}
