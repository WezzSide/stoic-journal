plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.settings"
}

dependencies {
    implementation(project(":core:notifications"))
    implementation(libs.androidx.activity.compose)
}
