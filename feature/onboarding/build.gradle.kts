plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.onboarding"
}

dependencies {
    implementation(project(":core:notifications"))
    implementation(libs.androidx.activity.compose)
}
