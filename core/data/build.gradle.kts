plugins {
    id("stoic.android.library")
    id("stoic.android.hilt")
    id("stoic.android.room")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.stoicjournal.core.data"
    buildFeatures {
        compose = false
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
    kapt(libs.androidx.room.compiler)

    testImplementation(libs.junit)
}
