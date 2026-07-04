plugins {
    id("stoic.android.library")
    id("stoic.android.hilt")
}

android {
    namespace = "com.stoicjournal.core.common"
    buildFeatures {
        compose = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
