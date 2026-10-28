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
    sourceSets {
        // Expose exported Room schemas to instrumentation tests (MigrationTestHelper).
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
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
    implementation(libs.okhttp.client)
    kapt(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
