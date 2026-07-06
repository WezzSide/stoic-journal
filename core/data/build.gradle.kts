plugins {
    id("stoic.android.library")
    id("stoic.android.hilt")
    id("stoic.android.room")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.stoicjournal.core.data"
    buildFeatures {
        compose = false
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.google.generative.ai)
    kapt(libs.androidx.room.compiler)

    testImplementation(libs.junit)
}
