plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.today"
}

dependencies {
    implementation(libs.androidx.material.icons.extended)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
