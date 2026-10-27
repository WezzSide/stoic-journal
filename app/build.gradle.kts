plugins {
    id("stoic.android.application")
}

android {
    namespace = "com.stoicjournal"

    defaultConfig {
        applicationId = "com.stoicjournal"
        testInstrumentationRunner = "com.stoicjournal.HiltTestRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("stoic-journal-release.keystore")
            storePassword = (project.findProperty("RELEASE_STORE_PASSWORD") as String?).takeUnless { it.isNullOrBlank() }
                ?: System.getenv("RELEASE_STORE_PASSWORD").takeUnless { it.isNullOrBlank() }
                ?: "android"
            keyAlias = (project.findProperty("RELEASE_KEY_ALIAS") as String?).takeUnless { it.isNullOrBlank() }
                ?: System.getenv("RELEASE_KEY_ALIAS").takeUnless { it.isNullOrBlank() }
                ?: "stoicjournal"
            keyPassword = (project.findProperty("RELEASE_KEY_PASSWORD") as String?).takeUnless { it.isNullOrBlank() }
                ?: System.getenv("RELEASE_KEY_PASSWORD").takeUnless { it.isNullOrBlank() }
                ?: "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:notifications"))
    implementation(project(":feature:today"))
    implementation(project(":feature:journal"))
    implementation(project(":feature:library"))
    implementation(project(":feature:history"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:ai"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.work)
    implementation(libs.androidx.compose.bom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.compiler)
}
