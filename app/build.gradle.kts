plugins {
    id("stoic.android.application")
}

android {
    namespace = "com.stoicjournal"

    defaultConfig {
        applicationId = "com.stoicjournal"
        testInstrumentationRunner = "com.stoicjournal.HiltTestRunner"
    }

    // Release signing is only configured when real credentials are supplied; there is deliberately
    // no silent fallback to default passwords. Without them the release build is UNSIGNED and the
    // build prints a warning, so a weak or wrong key can never be used by accident.
    val releaseStorePassword = (project.findProperty("RELEASE_STORE_PASSWORD") as String?)?.takeUnless { it.isBlank() }
        ?: System.getenv("RELEASE_STORE_PASSWORD")?.takeUnless { it.isBlank() }
    val releaseKeyAlias = (project.findProperty("RELEASE_KEY_ALIAS") as String?)?.takeUnless { it.isBlank() }
        ?: System.getenv("RELEASE_KEY_ALIAS")?.takeUnless { it.isBlank() }
    val releaseKeyPassword = (project.findProperty("RELEASE_KEY_PASSWORD") as String?)?.takeUnless { it.isBlank() }
        ?: System.getenv("RELEASE_KEY_PASSWORD")?.takeUnless { it.isBlank() }
    val releaseKeystore = rootProject.file("stoic-journal-release.keystore")
    val hasReleaseSigning = releaseKeystore.exists() &&
        releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        } else {
            logger.warn("Release signing credentials not provided (RELEASE_STORE_PASSWORD / RELEASE_KEY_ALIAS / RELEASE_KEY_PASSWORD); release artifacts will be UNSIGNED.")
        }
    }

    // Instrumentation tests run against a separate `qa` build (applicationId com.stoicjournal.qa) so the
    // E2E suite — which writes PINs, preferences and synthetic journal rows — can never touch the data of
    // an installed com.stoicjournal (debug or release) on a personal device. Run: connectedQaAndroidTest.
    testBuildType = "qa"
    // The qa build reuses the debug-only test host (HiltTestActivity + its manifest entry).
    sourceSets.maybeCreate("qa").apply {
        java.srcDir("src/debug/java")
        manifest.srcFile("src/debug/AndroidManifest.xml")
    }

    buildTypes {
        create("qa") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            matchingFallbacks += listOf("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.uiautomator)
    kaptAndroidTest(libs.hilt.compiler)
}
