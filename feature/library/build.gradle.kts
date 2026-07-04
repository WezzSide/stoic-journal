plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.library"
}

dependencies {
    implementation(libs.androidx.foundation)
}
