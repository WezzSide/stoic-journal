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
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
