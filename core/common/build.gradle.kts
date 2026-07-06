plugins {
    id("stoic.android.library")
    id("stoic.android.hilt")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.stoicjournal.core.common"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
}
