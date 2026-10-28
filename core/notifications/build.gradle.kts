plugins {
    id("stoic.android.library")
    id("stoic.android.hilt")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.stoicjournal.core.notifications"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.work)
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    ksp(libs.androidx.hilt.compiler)
}
