plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.library"
    lint {
        // Compose lint 1.6.x (BOM 2024.06.00) ComposableCoroutineCreationDetector throws an NPE on this
        // module under AGP 8.13 lint (see docs/final-qa/QA03). Only this single check is disabled, only here.
        disable += "CoroutineCreationDuringComposition"
    }
}

dependencies {
    implementation(libs.androidx.foundation)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
