// Phone-side bridge app (M2): receives the navigation target from c:geo via
// a geo: intent, owns GPS, pushes target + distance/bearing ticks to the
// watch over the Data Layer. Stub module for now — only the app shell exists.
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.tomasloksa.cgeowear.bridge"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.tomasloksa.cgeowear"
        minSdk = 26 // matches c:geo for a painless later merge
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
    // M2: libs.play.services.wearable
}
