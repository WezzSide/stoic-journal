plugins {
    id("stoic.android.library")
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
