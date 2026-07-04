## Task 1: Initialize Gradle project with convention plugins and version catalog

**Files:**
- Create: `settings.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `build.gradle.kts` (root)
- Create: `build-logic/convention/build.gradle.kts`
- Create: `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt`
- Create: `build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt`
- Create: `build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt`
- Create: `build-logic/convention/src/main/kotlin/AndroidHiltConventionPlugin.kt`
- Create: `build-logic/convention/src/main/kotlin/AndroidRoomConventionPlugin.kt`
- Create: `build-logic/settings.gradle.kts`
- Create: `gradle.properties`

**Interfaces:**
- Produces: Root project and module structure ready for Kotlin Android modules.

- [ ] **Step 1: Write root `settings.gradle.kts`**

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "StoicJournal"
include(":app")
include(":feature:today")
include(":feature:journal")
include(":feature:library")
include(":feature:history")
include(":feature:settings")
include(":feature:onboarding")
include(":core:data")
include(":core:domain")
include(":core:common")
include(":core:notifications")
```

- [ ] **Step 2: Write `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.5.0"
kotlin = "2.0.0"
coreKtx = "1.13.1"
lifecycle = "2.8.2"
activityCompose = "1.9.0"
composeBom = "2024.06.00"
navigation = "2.7.7"
hilt = "2.51.1"
hiltNavigation = "1.2.0"
room = "2.6.1"
datastore = "1.1.1"
work = "2.9.0"
junit = "4.13.2"
junitExt = "1.2.1"
espresso = "3.6.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigation" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitExt" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
android-gradlePlugin = { group = "com.android.tools.build", name = "gradle", version.ref = "agp" }
kotlin-gradlePlugin = { group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version.ref = "kotlin" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
stoic-android-application = { id = "stoic.android.application", version = "unspecified" }
stoic-android-library = { id = "stoic.android.library", version = "unspecified" }
stoic-android-feature = { id = "stoic.android.feature", version = "unspecified" }
stoic-android-hilt = { id = "stoic.android.hilt", version = "unspecified" }
stoic-android-room = { id = "stoic.android.room", version = "unspecified" }
```

- [ ] **Step 3: Write root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
}
```

- [ ] **Step 4: Write `build-logic/settings.gradle.kts`**

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
rootProject.name = "build-logic"
include(":convention")
```

- [ ] **Step 5: Write `build-logic/convention/build.gradle.kts`**

```kotlin
plugins {
    `kotlin-dsl`
}

group = "com.stoicjournal.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "stoic.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "stoic.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "stoic.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidHilt") {
            id = "stoic.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "stoic.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
    }
}
```

- [ ] **Step 6: Write `AndroidApplicationConventionPlugin.kt`**

```kotlin
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
            apply("org.jetbrains.kotlin.plugin.compose")
            apply("stoic.android.hilt")
        }
        extensions.configure<ApplicationExtension> {
            compileSdk = 34
            defaultConfig {
                minSdk = 26
                targetSdk = 34
                versionCode = 1
                versionName = "1.0"
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            kotlinOptions {
                jvmTarget = "17"
            }
            buildFeatures {
                compose = true
            }
            composeOptions {
                kotlinCompilerExtensionVersion = "1.5.14"
            }
        }
    }
}
```

- [ ] **Step 7: Write `AndroidLibraryConventionPlugin.kt`**

```kotlin
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.library")
            apply("org.jetbrains.kotlin.android")
        }
        extensions.configure<LibraryExtension> {
            compileSdk = 34
            defaultConfig {
                minSdk = 26
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            kotlinOptions {
                jvmTarget = "17"
            }
            buildFeatures {
                compose = true
            }
            composeOptions {
                kotlinCompilerExtensionVersion = "1.5.14"
            }
        }
    }
}
```

- [ ] **Step 8: Write `AndroidFeatureConventionPlugin.kt`**

```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("stoic.android.library")
            apply("stoic.android.hilt")
        }
        dependencies {
            "implementation"(project(":core:domain"))
            "implementation"(project(":core:common"))
            "implementation"(libs.androidx.lifecycle.viewmodel.compose)
            "implementation"(libs.androidx.lifecycle.runtime.compose)
            "implementation"(libs.androidx.navigation.compose)
            "implementation"(libs.hilt.navigation.compose)
            "implementation"(libs.androidx.ui)
            "implementation"(libs.androidx.ui.graphics)
            "implementation"(libs.androidx.ui.tooling.preview)
            "implementation"(libs.androidx.material3)
        }
    }
}
```

- [ ] **Step 9: Write `AndroidHiltConventionPlugin.kt`**

```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.google.dagger.hilt.android")
            apply("org.jetbrains.kotlin.kapt")
        }
        dependencies {
            "implementation"(libs.hilt.android)
            "kapt"(libs.hilt.compiler)
        }
    }
}
```

- [ ] **Step 10: Write `AndroidRoomConventionPlugin.kt`**

```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.kapt")
        }
        dependencies {
            "implementation"(libs.androidx.room.runtime)
            "implementation"(libs.androidx.room.ktx)
            "kapt"(libs.androidx.room.compiler)
        }
    }
}
```

- [ ] **Step 11: Write `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 12: Run a Gradle sync to validate project setup**

Run: `./gradlew projects`
Expected: `BUILD SUCCESSFUL` and all modules listed.

- [ ] **Step 13: Commit**

```bash
git add .
git commit -m "build: initialize Gradle project with convention plugins and version catalog"
```

---

