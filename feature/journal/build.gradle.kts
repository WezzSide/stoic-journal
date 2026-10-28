plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.journal"

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.material.icons.extended)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
