plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.ai"
}

dependencies {
    implementation(libs.androidx.material.icons.extended)
    
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    implementation(project(":core:domain"))
    implementation(project(":core:common"))

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
