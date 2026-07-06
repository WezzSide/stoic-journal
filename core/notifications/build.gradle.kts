plugins {
    id("stoic.android.library")
    id("stoic.android.hilt")
}

android {
    namespace = "com.stoicjournal.core.notifications"
    buildFeatures {
        compose = false
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.work)
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    kapt(libs.androidx.hilt.compiler)
}
