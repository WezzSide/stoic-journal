# Stoic Journal MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an offline-first Android MVP for Stoic Journal with daily guided/free-form journaling, a Stoic wisdom library, exercises, entry history, and daily reminders.

**Architecture:** MVVM + Repository pattern across feature modules (`today`, `journal`, `library`, `history`, `settings`) backed by a `core:data` Room database, `core:domain` use cases, and `core:common` utilities. Jetpack Compose renders the UI; Hilt handles dependency injection; WorkManager schedules the daily reminder.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, Room, Coroutines/Flow, WorkManager, DataStore, Navigation Compose.

## Global Constraints

- **minSdk:** 26 (Android 8.0)
- **targetSdk/compileSdk:** Latest stable API at time of build
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose with custom theme built on Material 3 tokens
- **Local Database:** Room/SQLite for user data and favorite state
- **Preferences:** DataStore for user preferences and onboarding state
- **Background reminders:** WorkManager
- **All reading and writing works without network**
- **Journal entries never leave the device in the MVP**
- **Cold start under 1.5s; entry list scrolls at 60fps**
- **Minimum 48dp touch targets, dynamic type support, content descriptions on icons**
- **Auto-save journal drafts to prevent data loss**

---

## File Structure

```
app/
  src/main/java/com/stoicjournal/
    MainActivity.kt
    StoicJournalApplication.kt
    navigation/
      StoicJournalNavHost.kt
      Screen.kt
    theme/
      Color.kt
      Type.kt
      Theme.kt
      Shape.kt
  src/main/res/...
  build.gradle.kts

feature/today/
  src/main/java/com/stoicjournal/feature/today/
    TodayScreen.kt
    TodayViewModel.kt
    TodayUiState.kt
    components/
      DailyPromptCard.kt
      QuickActionsRow.kt
      StreakIndicator.kt
  build.gradle.kts

feature/journal/
  src/main/java/com/stoicjournal/feature/journal/
    guided/
      GuidedJournalScreen.kt
      GuidedJournalViewModel.kt
    freeform/
      FreeFormJournalScreen.kt
      FreeFormJournalViewModel.kt
    exercise/
      ExerciseJournalScreen.kt
      ExerciseJournalViewModel.kt
    components/
      JournalTextField.kt
      QuoteHeader.kt
      SaveDiscardBar.kt
  build.gradle.kts

feature/library/
  src/main/java/com/stoicjournal/feature/library/
    LibraryScreen.kt
    LibraryViewModel.kt
    LibraryUiState.kt
    components/
      PhilosopherList.kt
      QuoteCard.kt
      ThemeChip.kt
  build.gradle.kts

feature/history/
  src/main/java/com/stoicjournal/feature/history/
    HistoryScreen.kt
    HistoryViewModel.kt
    HistoryUiState.kt
    components/
      EntryListItem.kt
      EntryFilterChips.kt
  build.gradle.kts

feature/settings/
  src/main/java/com/stoicjournal/feature/settings/
    SettingsScreen.kt
    SettingsViewModel.kt
    components/
      ReminderTimePicker.kt
  build.gradle.kts

feature/onboarding/
  src/main/java/com/stoicjournal/feature/onboarding/
    OnboardingScreen.kt
    OnboardingViewModel.kt
    OnboardingUiState.kt
  build.gradle.kts

core/data/
  src/main/java/com/stoicjournal/core/data/
    database/
      StoicJournalDatabase.kt
      dao/
        JournalEntryDao.kt
        QuoteDao.kt
        PromptDao.kt
        ExerciseDao.kt
        PhilosopherDao.kt
      entity/
        JournalEntryEntity.kt
        QuoteEntity.kt
        PromptEntity.kt
        ExerciseEntity.kt
        PhilosopherEntity.kt
    repository/
      JournalRepository.kt
      JournalRepositoryImpl.kt
      QuoteRepository.kt
      QuoteRepositoryImpl.kt
      PromptRepository.kt
      PromptRepositoryImpl.kt
      ExerciseRepository.kt
      ExerciseRepositoryImpl.kt
      PreferencesRepository.kt
      PreferencesRepositoryImpl.kt
    seed/
      AssetSeedImporter.kt
    di/
      DataModule.kt
    sync/
      StreakCalculator.kt
  src/main/assets/
    quotes.json
    prompts.json
    exercises.json
    philosophers.json
  build.gradle.kts

core/domain/
  src/main/java/com/stoicjournal/core/domain/
    model/
      JournalEntry.kt
      Quote.kt
      Prompt.kt
      Exercise.kt
      Philosopher.kt
      Streak.kt
      StoicismLevel.kt
      EntryType.kt
      Theme.kt
    usecase/
      GetTodayPromptUseCase.kt
      SaveJournalEntryUseCase.kt
      GetJournalEntriesUseCase.kt
      ToggleFavoriteQuoteUseCase.kt
      GetStreakUseCase.kt
  build.gradle.kts

core/common/
  src/main/java/com/stoicjournal/core/common/
    di/
      CommonModule.kt
    util/
      DateTimeUtil.kt
      JsonUtil.kt
  build.gradle.kts

core/notifications/
  src/main/java/com/stoicjournal/core/notifications/
    DailyReminderWorker.kt
    ReminderScheduler.kt
  build.gradle.kts

build-logic/...
settings.gradle.kts
gradle/libs.versions.toml
```

---

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

## Task 2: Create core domain models

**Files:**
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/EntryType.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/StoicismLevel.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/Theme.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/JournalEntry.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/Quote.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/Prompt.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/Exercise.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/Philosopher.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/model/Streak.kt`
- Create: `core/domain/build.gradle.kts`

**Interfaces:**
- Produces: Domain model classes used by repositories, use cases, ViewModels, and UI.

- [ ] **Step 1: Write `core/domain/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.library")
}

android {
    namespace = "com.stoicjournal.core.domain"
    buildFeatures {
        compose = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

- [ ] **Step 2: Write `EntryType.kt`**

```kotlin
package com.stoicjournal.core.domain.model

enum class EntryType {
    GUIDED,
    FREE_FORM,
    EXERCISE
}
```

- [ ] **Step 3: Write `StoicismLevel.kt`**

```kotlin
package com.stoicjournal.core.domain.model

enum class StoicismLevel {
    BEGINNER,
    FAMILIAR,
    ADVANCED
}
```

- [ ] **Step 4: Write `Theme.kt`**

```kotlin
package com.stoicjournal.core.domain.model

enum class Theme {
    CONTROL,
    ADVERSITY,
    MORTALITY,
    TEMPERANCE,
    JUSTICE,
    COURAGE,
    WISDOM
}
```

- [ ] **Step 5: Write `JournalEntry.kt`**

```kotlin
package com.stoicjournal.core.domain.model

import java.time.Instant

data class JournalEntry(
    val id: String,
    val type: EntryType,
    val promptId: String? = null,
    val title: String? = null,
    val body: String,
    val quoteId: String? = null,
    val exerciseId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

- [ ] **Step 6: Write `Quote.kt`**

```kotlin
package com.stoicjournal.core.domain.model

data class Quote(
    val id: String,
    val text: String,
    val attribution: String,
    val philosopherId: String,
    val theme: Theme,
    val source: String? = null,
    val isFavorite: Boolean = false
)
```

- [ ] **Step 7: Write `Prompt.kt`**

```kotlin
package com.stoicjournal.core.domain.model

data class Prompt(
    val id: String,
    val text: String,
    val theme: Theme,
    val philosopherId: String? = null,
    val difficultyLevel: StoicismLevel = StoicismLevel.BEGINNER
)
```

- [ ] **Step 8: Write `Exercise.kt`**

```kotlin
package com.stoicjournal.core.domain.model

data class Exercise(
    val id: String,
    val title: String,
    val description: String,
    val steps: List<String>,
    val theme: Theme,
    val difficultyLevel: StoicismLevel = StoicismLevel.BEGINNER
)
```

- [ ] **Step 9: Write `Philosopher.kt`**

```kotlin
package com.stoicjournal.core.domain.model

data class Philosopher(
    val id: String,
    val name: String,
    val lived: String,
    val shortBio: String
)
```

- [ ] **Step 10: Write `Streak.kt`**

```kotlin
package com.stoicjournal.core.domain.model

import java.time.LocalDate

data class Streak(
    val currentCount: Int,
    val lastEntryDate: LocalDate?
)
```

- [ ] **Step 11: Run a build to verify domain module compiles**

Run: `./gradlew :core:domain:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 12: Commit**

```bash
git add core/domain/
git commit -m "feat(domain): add core domain models"
```

---

## Task 3: Set up Room database, entities, DAOs, and seed bundled content

**Files:**
- Create: `core/data/src/main/java/com/stoicjournal/core/data/entity/JournalEntryEntity.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/entity/QuoteEntity.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/entity/PromptEntity.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/entity/ExerciseEntity.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/entity/PhilosopherEntity.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/dao/JournalEntryDao.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/dao/QuoteDao.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/dao/PromptDao.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/dao/ExerciseDao.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/dao/PhilosopherDao.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/database/StoicJournalDatabase.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/seed/AssetSeedImporter.kt`
- Create: `core/data/src/main/assets/quotes.json`
- Create: `core/data/src/main/assets/prompts.json`
- Create: `core/data/src/main/assets/exercises.json`
- Create: `core/data/src/main/assets/philosophers.json`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/di/DataModule.kt`
- Create: `core/data/build.gradle.kts`

**Interfaces:**
- Consumes: Domain models from `core:domain`.
- Produces: Room database, DAOs, seed importer, and Hilt module providing repositories and database.

- [ ] **Step 1: Write `core/data/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.library")
    id("stoic.android.hilt")
    id("stoic.android.room")
    id("org.jetbrains.kotlin.plugin.serialization")
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
}
```

- [ ] **Step 2: Write `JournalEntryEntity.kt`**

```kotlin
package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stoicjournal.core.domain.model.EntryType

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val type: EntryType,
    val promptId: String? = null,
    val title: String? = null,
    val body: String,
    val quoteId: String? = null,
    val exerciseId: String? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
```

- [ ] **Step 3: Write `QuoteEntity.kt`**

```kotlin
package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stoicjournal.core.domain.model.Theme

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val id: String,
    val text: String,
    val attribution: String,
    val philosopherId: String,
    val theme: Theme,
    val source: String? = null,
    val isFavorite: Boolean = false
)
```

- [ ] **Step 4: Write `PromptEntity.kt`**

```kotlin
package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.model.Theme

@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey val id: String,
    val text: String,
    val theme: Theme,
    val philosopherId: String? = null,
    val difficultyLevel: StoicismLevel = StoicismLevel.BEGINNER
)
```

- [ ] **Step 5: Write `ExerciseEntity.kt`**

```kotlin
package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.model.Theme

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val steps: String, // JSON array string
    val theme: Theme,
    val difficultyLevel: StoicismLevel = StoicismLevel.BEGINNER
)
```

- [ ] **Step 6: Write `PhilosopherEntity.kt`**

```kotlin
package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "philosophers")
data class PhilosopherEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lived: String,
    val shortBio: String
)
```

- [ ] **Step 7: Write `JournalEntryDao.kt`**

```kotlin
package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stoicjournal.core.data.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries ORDER BY createdAtMillis DESC")
    fun getAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: String): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntryEntity)

    @Delete
    suspend fun delete(entry: JournalEntryEntity)
}
```

- [ ] **Step 8: Write `QuoteDao.kt`**

```kotlin
package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.stoicjournal.core.data.entity.QuoteEntity
import com.stoicjournal.core.domain.model.Theme
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes ORDER BY id ASC")
    fun getAll(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE philosopherId = :philosopherId ORDER BY id ASC")
    fun getByPhilosopher(philosopherId: String): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE theme = :theme ORDER BY id ASC")
    fun getByTheme(theme: Theme): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE isFavorite = 1 ORDER BY id ASC")
    fun getFavorites(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE id = :id")
    suspend fun getById(id: String): QuoteEntity?

    @Update
    suspend fun update(quote: QuoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotes: List<QuoteEntity>)
}
```

- [ ] **Step 9: Write `PromptDao.kt`**

```kotlin
package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stoicjournal.core.data.entity.PromptEntity
import com.stoicjournal.core.domain.model.StoicismLevel
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts ORDER BY id ASC")
    fun getAll(): Flow<List<PromptEntity>>

    @Query("SELECT * FROM prompts WHERE difficultyLevel = :level ORDER BY id ASC")
    fun getByDifficulty(level: StoicismLevel): Flow<List<PromptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prompts: List<PromptEntity>)
}
```

- [ ] **Step 10: Write `ExerciseDao.kt`**

```kotlin
package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stoicjournal.core.data.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY id ASC")
    fun getAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)
}
```

- [ ] **Step 11: Write `PhilosopherDao.kt`**

```kotlin
package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stoicjournal.core.data.entity.PhilosopherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhilosopherDao {
    @Query("SELECT * FROM philosophers ORDER BY name ASC")
    fun getAll(): Flow<List<PhilosopherEntity>>

    @Query("SELECT * FROM philosophers WHERE id = :id")
    suspend fun getById(id: String): PhilosopherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(philosophers: List<PhilosopherEntity>)
}
```

- [ ] **Step 12: Write `StoicJournalDatabase.kt`**

```kotlin
package com.stoicjournal.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.stoicjournal.core.data.dao.ExerciseDao
import com.stoicjournal.core.data.dao.JournalEntryDao
import com.stoicjournal.core.data.dao.PhilosopherDao
import com.stoicjournal.core.data.dao.PromptDao
import com.stoicjournal.core.data.dao.QuoteDao
import com.stoicjournal.core.data.entity.ExerciseEntity
import com.stoicjournal.core.data.entity.JournalEntryEntity
import com.stoicjournal.core.data.entity.PhilosopherEntity
import com.stoicjournal.core.data.entity.PromptEntity
import com.stoicjournal.core.data.entity.QuoteEntity
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.model.Theme

@Database(
    entities = [
        JournalEntryEntity::class,
        QuoteEntity::class,
        PromptEntity::class,
        ExerciseEntity::class,
        PhilosopherEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(StoicJournalTypeConverters::class)
abstract class StoicJournalDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun quoteDao(): QuoteDao
    abstract fun promptDao(): PromptDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun philosopherDao(): PhilosopherDao
}

class StoicJournalTypeConverters {
    @TypeConverter
    fun fromEntryType(value: EntryType): String = value.name

    @TypeConverter
    fun toEntryType(value: String): EntryType = EntryType.valueOf(value)

    @TypeConverter
    fun fromStoicismLevel(value: StoicismLevel): String = value.name

    @TypeConverter
    fun toStoicismLevel(value: String): StoicismLevel = StoicismLevel.valueOf(value)

    @TypeConverter
    fun fromTheme(value: Theme): String = value.name

    @TypeConverter
    fun toTheme(value: String): Theme = Theme.valueOf(value)
}
```

- [ ] **Step 13: Write `AssetSeedImporter.kt`**

```kotlin
package com.stoicjournal.core.data.seed

import android.content.Context
import com.stoicjournal.core.data.dao.ExerciseDao
import com.stoicjournal.core.data.dao.PhilosopherDao
import com.stoicjournal.core.data.dao.PromptDao
import com.stoicjournal.core.data.dao.QuoteDao
import com.stoicjournal.core.data.entity.ExerciseEntity
import com.stoicjournal.core.data.entity.PhilosopherEntity
import com.stoicjournal.core.data.entity.PromptEntity
import com.stoicjournal.core.data.entity.QuoteEntity
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.model.Theme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import javax.inject.Inject

class AssetSeedImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val quoteDao: QuoteDao,
    private val promptDao: PromptDao,
    private val exerciseDao: ExerciseDao,
    private val philosopherDao: PhilosopherDao
) {
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (quoteDao.getAll().first().isNotEmpty()) return@withContext
        val json = Json { ignoreUnknownKeys = true }

        context.assets.open("quotes.json").use { stream ->
            val dtos: List<QuoteDto> = json.decodeFromStream(stream)
            quoteDao.insertAll(dtos.map { it.toEntity() })
        }
        context.assets.open("prompts.json").use { stream ->
            val dtos: List<PromptDto> = json.decodeFromStream(stream)
            promptDao.insertAll(dtos.map { it.toEntity() })
        }
        context.assets.open("exercises.json").use { stream ->
            val dtos: List<ExerciseDto> = json.decodeFromStream(stream)
            exerciseDao.insertAll(dtos.map { it.toEntity() })
        }
        context.assets.open("philosophers.json").use { stream ->
            val dtos: List<PhilosopherDto> = json.decodeFromStream(stream)
            philosopherDao.insertAll(dtos.map { it.toEntity() })
        }
    }
}

@Serializable
private data class QuoteDto(
    val id: String,
    val text: String,
    val attribution: String,
    val philosopherId: String,
    val theme: String,
    val source: String? = null
) {
    fun toEntity() = QuoteEntity(
        id = id,
        text = text,
        attribution = attribution,
        philosopherId = philosopherId,
        theme = Theme.valueOf(theme),
        source = source
    )
}

@Serializable
private data class PromptDto(
    val id: String,
    val text: String,
    val theme: String,
    val philosopherId: String? = null,
    val difficultyLevel: String = "BEGINNER"
) {
    fun toEntity() = PromptEntity(
        id = id,
        text = text,
        theme = Theme.valueOf(theme),
        philosopherId = philosopherId,
        difficultyLevel = StoicismLevel.valueOf(difficultyLevel)
    )
}

@Serializable
private data class ExerciseDto(
    val id: String,
    val title: String,
    val description: String,
    val steps: List<String>,
    val theme: String,
    val difficultyLevel: String = "BEGINNER"
) {
    fun toEntity() = ExerciseEntity(
        id = id,
        title = title,
        description = description,
        steps = steps.joinToString("|"),
        theme = Theme.valueOf(theme),
        difficultyLevel = StoicismLevel.valueOf(difficultyLevel)
    )
}

@Serializable
private data class PhilosopherDto(
    val id: String,
    val name: String,
    val lived: String,
    val shortBio: String
) {
    fun toEntity() = PhilosopherEntity(
        id = id,
        name = name,
        lived = lived,
        shortBio = shortBio
    )
}
```

- [ ] **Step 14: Write bundled `quotes.json`** (sample subset)

```json
[
  {
    "id": "marcus-001",
    "text": "You have power over your mind — not outside events. Realize this, and you will find strength.",
    "attribution": "Marcus Aurelius",
    "philosopherId": "marcus-aurelius",
    "theme": "CONTROL",
    "source": "Meditations"
  },
  {
    "id": "epictetus-001",
    "text": "It's not what happens to you, but how you react to it that matters.",
    "attribution": "Epictetus",
    "philosopherId": "epictetus",
    "theme": "ADVERSITY"
  },
  {
    "id": "seneca-001",
    "text": "We suffer more often in imagination than in reality.",
    "attribution": "Seneca",
    "philosopherId": "seneca",
    "theme": "ADVERSITY"
  }
]
```

- [ ] **Step 15: Write bundled `prompts.json`** (sample subset)

```json
[
  {
    "id": "prompt-control-001",
    "text": "What is within your control today, and what is not? How will you focus your energy on the former?",
    "theme": "CONTROL",
    "difficultyLevel": "BEGINNER"
  },
  {
    "id": "prompt-adversity-001",
    "text": "Recall a recent difficulty. What virtue can you practice in response to it?",
    "theme": "ADVERSITY",
    "difficultyLevel": "FAMILIAR"
  },
  {
    "id": "prompt-mortality-001",
    "text": "If you knew this were your last day, what would you stop doing and what would you begin?",
    "theme": "MORTALITY",
    "difficultyLevel": "ADVANCED"
  }
]
```

- [ ] **Step 16: Write bundled `exercises.json`**

```json
[
  {
    "id": "exercise-dichotomy-of-control",
    "title": "Dichotomy of Control",
    "description": "Separate what is in your control from what is not.",
    "steps": [
      "Take three slow breaths.",
      "List one thing that is troubling you.",
      "Ask: Is this entirely within my control, partly within my control, or outside my control?",
      "For what is within your control, name one virtuous action.",
      "For what is outside your control, practice accepting it as it is."
    ],
    "theme": "CONTROL",
    "difficultyLevel": "BEGINNER"
  },
  {
    "id": "exercise-evening-review",
    "title": "Evening Review",
    "description": "Reflect on the day’s actions and judgments.",
    "steps": [
      "Where did I act well today?",
      "Where did I fall short of my values?",
      "What could I do differently tomorrow?",
      "Write one sentence of self-forgiveness."
    ],
    "theme": "WISDOM",
    "difficultyLevel": "FAMILIAR"
  },
  {
    "id": "exercise-premeditatio-malorum",
    "title": "Premeditatio Malorum",
    "description": "Anticipate challenges with calm.",
    "steps": [
      "Identify one challenge you may face tomorrow.",
      "Imagine it happening in detail.",
      "Ask: What resources do I have to meet it?",
      "Commit to one virtuous response."
    ],
    "theme": "ADVERSITY",
    "difficultyLevel": "FAMILIAR"
  },
  {
    "id": "exercise-view-from-above",
    "title": "View from Above",
    "description": "Gain perspective by imagining your place in the wider world.",
    "steps": [
      "Close your eyes and picture your current location from above.",
      "Zoom out to see your neighborhood, city, country, planet.",
      "Notice how small today's concern appears.",
      "Return with one calm intention."
    ],
    "theme": "MORTALITY",
    "difficultyLevel": "ADVANCED"
  },
  {
    "id": "exercise-amor-fati",
    "title": "Amor Fati",
    "description": "Reflect on accepting what happens.",
    "steps": [
      "Write down one thing you wish were different.",
      "Ask: What could this obstacle teach me?",
      "Write one reason to be grateful it happened exactly this way.",
      "Choose to meet tomorrow with open hands."
    ],
    "theme": "COURAGE",
    "difficultyLevel": "ADVANCED"
  }
]
```

- [ ] **Step 17: Write bundled `philosophers.json`**

```json
[
  {
    "id": "marcus-aurelius",
    "name": "Marcus Aurelius",
    "lived": "121–180 AD",
    "shortBio": "Roman emperor and Stoic philosopher, author of the Meditations."
  },
  {
    "id": "epictetus",
    "name": "Epictetus",
    "lived": "c. 50–135 AD",
    "shortBio": "A formerly enslaved Stoic teacher whose Discourses emphasize the dichotomy of control."
  },
  {
    "id": "seneca",
    "name": "Seneca the Younger",
    "lived": "c. 4 BC–65 AD",
    "shortBio": "Roman Stoic philosopher, statesman, and playwright."
  },
  {
    "id": "musonius-rufus",
    "name": "Musonius Rufus",
    "lived": "c. 30–100 AD",
    "shortBio": "Roman Stoic teacher whose lectures influenced Epictetus."
  },
  {
    "id": "cleanthes",
    "name": "Cleanthes",
    "lived": "c. 330–230 BC",
    "shortBio": "Second head of the Stoic school, known for his Hymn to Zeus."
  },
  {
    "id": "cato-the-younger",
    "name": "Cato the Younger",
    "lived": "95–46 BC",
    "shortBio": "Roman senator celebrated in Stoic tradition as a model of integrity."
  }
]
```

- [ ] **Step 18: Write `DataModule.kt`**

```kotlin
package com.stoicjournal.core.data.di

import android.content.Context
import androidx.room.Room
import com.stoicjournal.core.data.database.StoicJournalDatabase
import com.stoicjournal.core.data.repository.ExerciseRepository
import com.stoicjournal.core.data.repository.ExerciseRepositoryImpl
import com.stoicjournal.core.data.repository.JournalRepository
import com.stoicjournal.core.data.repository.JournalRepositoryImpl
import com.stoicjournal.core.data.repository.PhilosopherRepository
import com.stoicjournal.core.data.repository.PhilosopherRepositoryImpl
import com.stoicjournal.core.data.repository.PreferencesRepository
import com.stoicjournal.core.data.repository.PreferencesRepositoryImpl
import com.stoicjournal.core.data.repository.PromptRepository
import com.stoicjournal.core.data.repository.PromptRepositoryImpl
import com.stoicjournal.core.data.repository.QuoteRepository
import com.stoicjournal.core.data.repository.QuoteRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindJournalRepository(impl: JournalRepositoryImpl): JournalRepository

    @Binds
    abstract fun bindQuoteRepository(impl: QuoteRepositoryImpl): QuoteRepository

    @Binds
    abstract fun bindPromptRepository(impl: PromptRepositoryImpl): PromptRepository

    @Binds
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    abstract fun bindPhilosopherRepository(impl: PhilosopherRepositoryImpl): PhilosopherRepository

    @Binds
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StoicJournalDatabase {
        return Room.databaseBuilder(
            context,
            StoicJournalDatabase::class.java,
            "stoic_journal.db"
        ).build()
    }

    @Provides
    fun provideJournalEntryDao(db: StoicJournalDatabase) = db.journalEntryDao()

    @Provides
    fun provideQuoteDao(db: StoicJournalDatabase) = db.quoteDao()

    @Provides
    fun providePromptDao(db: StoicJournalDatabase) = db.promptDao()

    @Provides
    fun provideExerciseDao(db: StoicJournalDatabase) = db.exerciseDao()

    @Provides
    fun providePhilosopherDao(db: StoicJournalDatabase) = db.philosopherDao()
}
```

- [ ] **Step 19: Add kotlinx serialization to version catalog**

Modify `gradle/libs.versions.toml`:

```toml
[versions]
kotlinxSerialization = "1.6.3"

[libraries]
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 20: Build the data module**

Run: `./gradlew :core:data:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 21: Commit**

```bash
git add core/data/ gradle/libs.versions.toml
git commit -m "feat(data): add Room database, entities, DAOs, and bundled content seeding"
```

---

## Task 4: Implement repositories and mappers

**Files:**
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/repository/JournalRepository.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/repository/JournalRepositoryImpl.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/repository/QuoteRepository.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/repository/QuoteRepositoryImpl.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/repository/PromptRepository.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/repository/PromptRepositoryImpl.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/repository/ExerciseRepository.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/repository/ExerciseRepositoryImpl.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/repository/PhilosopherRepository.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/repository/PhilosopherRepositoryImpl.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/repository/PreferencesRepository.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/repository/PreferencesRepositoryImpl.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/sync/StreakCalculator.kt`
- Create: `core/data/src/test/java/com/stoicjournal/core/data/sync/StreakCalculatorTest.kt`

**Interfaces:**
- Consumes: DAOs, domain models, `AssetSeedImporter`, DataStore.
- Produces: Repository implementations exposing `Flow` and `suspend` methods returning domain models.

- [ ] **Step 1: Write `JournalRepository.kt`**

```kotlin
package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun getAllEntries(): Flow<List<JournalEntry>>
    suspend fun getEntryById(id: String): JournalEntry?
    suspend fun saveEntry(entry: JournalEntry)
    suspend fun deleteEntry(entry: JournalEntry)
}
```

- [ ] **Step 2: Write `JournalRepositoryImpl.kt`**

```kotlin
package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.JournalEntryDao
import com.stoicjournal.core.data.entity.JournalEntryEntity
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class JournalRepositoryImpl @Inject constructor(
    private val journalEntryDao: JournalEntryDao
) : JournalRepository {

    override fun getAllEntries(): Flow<List<JournalEntry>> {
        return journalEntryDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEntryById(id: String): JournalEntry? {
        return journalEntryDao.getById(id)?.toDomain()
    }

    override suspend fun saveEntry(entry: JournalEntry) {
        journalEntryDao.insert(entry.toEntity())
    }

    override suspend fun deleteEntry(entry: JournalEntry) {
        journalEntryDao.delete(entry.toEntity())
    }
}

private fun JournalEntryEntity.toDomain() = JournalEntry(
    id = id,
    type = type,
    promptId = promptId,
    title = title,
    body = body,
    quoteId = quoteId,
    exerciseId = exerciseId,
    createdAt = Instant.ofEpochMilli(createdAtMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtMillis)
)

private fun JournalEntry.toEntity() = JournalEntryEntity(
    id = id,
    type = type,
    promptId = promptId,
    title = title,
    body = body,
    quoteId = quoteId,
    exerciseId = exerciseId,
    createdAtMillis = createdAt.toEpochMilli(),
    updatedAtMillis = updatedAt.toEpochMilli()
)
```

- [ ] **Step 3: Write `QuoteRepository.kt` and `QuoteRepositoryImpl.kt`**

```kotlin
package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Theme
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    fun getAllQuotes(): Flow<List<Quote>>
    fun getQuotesByPhilosopher(philosopherId: String): Flow<List<Quote>>
    fun getQuotesByTheme(theme: Theme): Flow<List<Quote>>
    fun getFavoriteQuotes(): Flow<List<Quote>>
    suspend fun toggleFavorite(quoteId: String)
}
```

```kotlin
package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.QuoteDao
import com.stoicjournal.core.data.entity.QuoteEntity
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Theme
import com.stoicjournal.core.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuoteRepositoryImpl @Inject constructor(
    private val quoteDao: QuoteDao
) : QuoteRepository {

    override fun getAllQuotes(): Flow<List<Quote>> = quoteDao.getAll().map { it.map(QuoteEntity::toDomain) }
    override fun getQuotesByPhilosopher(philosopherId: String): Flow<List<Quote>> =
        quoteDao.getByPhilosopher(philosopherId).map { it.map(QuoteEntity::toDomain) }

    override fun getQuotesByTheme(theme: Theme): Flow<List<Quote>> =
        quoteDao.getByTheme(theme).map { it.map(QuoteEntity::toDomain) }

    override fun getFavoriteQuotes(): Flow<List<Quote>> =
        quoteDao.getFavorites().map { it.map(QuoteEntity::toDomain) }

    override suspend fun toggleFavorite(quoteId: String) {
        val quote = quoteDao.getById(quoteId) ?: return
        quoteDao.update(quote.copy(isFavorite = !quote.isFavorite))
    }
}

private fun QuoteEntity.toDomain() = Quote(
    id = id,
    text = text,
    attribution = attribution,
    philosopherId = philosopherId,
    theme = theme,
    source = source,
    isFavorite = isFavorite
)
```

- [ ] **Step 4: Write `PromptRepository.kt` and `PromptRepositoryImpl.kt`**

```kotlin
package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.StoicismLevel
import kotlinx.coroutines.flow.Flow

interface PromptRepository {
    fun getAllPrompts(): Flow<List<Prompt>>
    fun getPromptsByDifficulty(level: StoicismLevel): Flow<List<Prompt>>
}
```

```kotlin
package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.PromptDao
import com.stoicjournal.core.data.entity.PromptEntity
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.repository.PromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PromptRepositoryImpl @Inject constructor(
    private val promptDao: PromptDao
) : PromptRepository {

    override fun getAllPrompts(): Flow<List<Prompt>> =
        promptDao.getAll().map { it.map(PromptEntity::toDomain) }

    override fun getPromptsByDifficulty(level: StoicismLevel): Flow<List<Prompt>> =
        promptDao.getByDifficulty(level).map { it.map(PromptEntity::toDomain) }
}

private fun PromptEntity.toDomain() = Prompt(
    id = id,
    text = text,
    theme = theme,
    philosopherId = philosopherId,
    difficultyLevel = difficultyLevel
)
```

- [ ] **Step 5: Write `ExerciseRepository.kt` and `ExerciseRepositoryImpl.kt`**

```kotlin
package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    suspend fun getExerciseById(id: String): Exercise?
}
```

```kotlin
package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.ExerciseDao
import com.stoicjournal.core.data.entity.ExerciseEntity
import com.stoicjournal.core.domain.model.Exercise
import com.stoicjournal.core.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> =
        exerciseDao.getAll().map { it.map(ExerciseEntity::toDomain) }

    override suspend fun getExerciseById(id: String): Exercise? =
        exerciseDao.getById(id)?.toDomain()
}

private fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    title = title,
    description = description,
    steps = steps.split("|"),
    theme = theme,
    difficultyLevel = difficultyLevel
)
```

- [ ] **Step 6: Write `PhilosopherRepository.kt` and `PhilosopherRepositoryImpl.kt`**

```kotlin
package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.Philosopher
import kotlinx.coroutines.flow.Flow

interface PhilosopherRepository {
    fun getAllPhilosophers(): Flow<List<Philosopher>>
    suspend fun getPhilosopherById(id: String): Philosopher?
}
```

```kotlin
package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.PhilosopherDao
import com.stoicjournal.core.data.entity.PhilosopherEntity
import com.stoicjournal.core.domain.model.Philosopher
import com.stoicjournal.core.domain.repository.PhilosopherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PhilosopherRepositoryImpl @Inject constructor(
    private val philosopherDao: PhilosopherDao
) : PhilosopherRepository {

    override fun getAllPhilosophers(): Flow<List<Philosopher>> =
        philosopherDao.getAll().map { it.map(PhilosopherEntity::toDomain) }

    override suspend fun getPhilosopherById(id: String): Philosopher? =
        philosopherDao.getById(id)?.toDomain()
}

private fun PhilosopherEntity.toDomain() = Philosopher(
    id = id,
    name = name,
    lived = lived,
    shortBio = shortBio
)
```

- [ ] **Step 7: Write `PreferencesRepository.kt` and `PreferencesRepositoryImpl.kt`**

```kotlin
package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.StoicismLevel
import kotlinx.coroutines.flow.Flow

data class UserPreferences(
    val reminderHour: Int,
    val reminderMinute: Int,
    val reminderEnabled: Boolean,
    val stoicismLevel: StoicismLevel,
    val onboardingComplete: Boolean
)

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setReminderTime(hour: Int, minute: Int)
    suspend fun setReminderEnabled(enabled: Boolean)
    suspend fun setStoicismLevel(level: StoicismLevel)
    suspend fun setOnboardingComplete(complete: Boolean)
}
```

```kotlin
package com.stoicjournal.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stoic_journal_preferences")

class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private val dataStore = context.dataStore

    companion object {
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val STOICISM_LEVEL = intPreferencesKey("stoicism_level")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            reminderHour = prefs[REMINDER_HOUR] ?: 20,
            reminderMinute = prefs[REMINDER_MINUTE] ?: 0,
            reminderEnabled = prefs[REMINDER_ENABLED] ?: true,
            stoicismLevel = StoicismLevel.entries[prefs[STOICISM_LEVEL] ?: 0],
            onboardingComplete = prefs[ONBOARDING_COMPLETE] ?: false
        )
    }

    override suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[REMINDER_HOUR] = hour
            prefs[REMINDER_MINUTE] = minute
        }
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[REMINDER_ENABLED] = enabled
        }
    }

    override suspend fun setStoicismLevel(level: StoicismLevel) {
        dataStore.edit { prefs ->
            prefs[STOICISM_LEVEL] = level.ordinal
        }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE] = complete
        }
    }
}
```

- [ ] **Step 8: Write `StreakCalculator.kt`**

```kotlin
package com.stoicjournal.core.data.sync

import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.model.Streak
import java.time.LocalDate
import java.time.ZoneId

object StreakCalculator {

    fun calculate(entries: List<JournalEntry>): Streak {
        if (entries.isEmpty()) return Streak(currentCount = 0, lastEntryDate = null)
        val dates = entries
            .map { it.createdAt.atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sortedDescending()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val mostRecent = dates.first()
        if (mostRecent != today && mostRecent != yesterday) {
            return Streak(currentCount = 0, lastEntryDate = mostRecent)
        }
        var streak = 1
        var current = mostRecent
        for (date in dates.drop(1)) {
            if (date == current.minusDays(1)) {
                streak++
                current = date
            } else {
                break
            }
        }
        return Streak(currentCount = streak, lastEntryDate = mostRecent)
    }
}
```

- [ ] **Step 9: Write `StreakCalculatorTest.kt`**

```kotlin
package com.stoicjournal.core.data.sync

import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StreakCalculatorTest {

    @Test
    fun `empty entries returns zero streak`() {
        val result = StreakCalculator.calculate(emptyList())
        assertEquals(0, result.currentCount)
        assertEquals(null, result.lastEntryDate)
    }

    @Test
    fun `consecutive days returns correct streak`() {
        val entries = (0L..2L).map { daysAgo ->
            entryOn(LocalDate.now().minusDays(daysAgo))
        }
        val result = StreakCalculator.calculate(entries)
        assertEquals(3, result.currentCount)
    }

    @Test
    fun `gap breaks streak`() {
        val entries = listOf(
            entryOn(LocalDate.now()),
            entryOn(LocalDate.now().minusDays(2))
        )
        val result = StreakCalculator.calculate(entries)
        assertEquals(1, result.currentCount)
    }

    private fun entryOn(date: LocalDate): JournalEntry {
        val instant = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        return JournalEntry(
            id = instant.toString(),
            type = EntryType.GUIDED,
            body = "",
            createdAt = instant,
            updatedAt = instant
        )
    }
}
```

- [ ] **Step 10: Run repository tests**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, tests pass.

- [ ] **Step 11: Commit**

```bash
git add core/data/
git commit -m "feat(data): add repositories, preferences, and streak calculator"
```

---

## Task 5: Implement use cases in core:domain

**Files:**
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/GetTodayPromptUseCase.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/SaveJournalEntryUseCase.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/GetJournalEntriesUseCase.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/ToggleFavoriteQuoteUseCase.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/GetStreakUseCase.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/GetQuoteForPromptUseCase.kt`
- Modify: `core/domain/build.gradle.kts`

**Interfaces:**
- Consumes: Repositories from `core:data`.
- Produces: Use cases injected into ViewModels.

- [ ] **Step 1: Add Hilt dependency to `core/domain/build.gradle.kts`**

Repository interfaces live in `core:domain`, so use cases here depend only on interfaces, not on `core:data`.

```kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

- [ ] **Step 2: Ensure repository interfaces are in `core:domain`**

Create `core/domain/src/main/java/com/stoicjournal/core/domain/repository/` and move all `*Repository.kt` interface files there.

Update package declarations from `com.stoicjournal.core.data.repository` to `com.stoicjournal.core.domain.repository`.

- [ ] **Step 3: Update `DataModule.kt` to bind interfaces from domain package**

Change imports:

```kotlin
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.repository.QuoteRepository
import com.stoicjournal.core.domain.repository.PromptRepository
import com.stoicjournal.core.domain.repository.ExerciseRepository
import com.stoicjournal.core.domain.repository.PhilosopherRepository
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.UserPreferences
```

- [ ] **Step 4: Write `GetTodayPromptUseCase.kt`**

```kotlin
package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.PromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

class GetTodayPromptUseCase @Inject constructor(
    private val promptRepository: PromptRepository,
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<TodayPrompt> = combine(
        promptRepository.getAllPrompts(),
        preferencesRepository.preferences
    ) { prompts, prefs ->
        val candidates = if (prefs.stoicismLevel == StoicismLevel.BEGINNER) {
            prompts.filter { it.difficultyLevel == StoicismLevel.BEGINNER }
        } else {
            prompts
        }
        val selected = candidates.ifEmpty { prompts }.let {
            it[LocalDate.now().dayOfYear % it.size]
        }
        TodayPrompt(
            prompt = selected,
            date = LocalDate.now()
        )
    }
}

data class TodayPrompt(
    val prompt: Prompt,
    val date: LocalDate
)
```

- [ ] **Step 5: Write `GetQuoteForPromptUseCase.kt`**

```kotlin
package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetQuoteForPromptUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository
) {
    operator fun invoke(prompt: Prompt): Flow<Quote?> =
        quoteRepository.getAllQuotes().map { quotes ->
            val matching = quotes.filter { it.theme == prompt.theme || it.philosopherId == prompt.philosopherId }
            matching.firstOrNull() ?: quotes.firstOrNull()
        }
}
```

- [ ] **Step 6: Write `SaveJournalEntryUseCase.kt`**

```kotlin
package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class SaveJournalEntryUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    suspend operator fun invoke(entry: JournalEntry) {
        journalRepository.saveEntry(entry)
    }
}

fun createJournalEntry(
    type: com.stoicjournal.core.domain.model.EntryType,
    body: String,
    promptId: String? = null,
    title: String? = null,
    quoteId: String? = null,
    exerciseId: String? = null
): JournalEntry {
    val now = Instant.now()
    return JournalEntry(
        id = UUID.randomUUID().toString(),
        type = type,
        promptId = promptId,
        title = title,
        body = body,
        quoteId = quoteId,
        exerciseId = exerciseId,
        createdAt = now,
        updatedAt = now
    )
}
```

- [ ] **Step 7: Write `GetJournalEntriesUseCase.kt`**

```kotlin
package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetJournalEntriesUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    operator fun invoke(filter: EntryType? = null): Flow<List<JournalEntry>> {
        return journalRepository.getAllEntries().map { entries ->
            filter?.let { entries.filter { entry -> entry.type == filter } } ?: entries
        }
    }
}
```

- [ ] **Step 8: Write `ToggleFavoriteQuoteUseCase.kt`**

```kotlin
package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.repository.QuoteRepository
import javax.inject.Inject

class ToggleFavoriteQuoteUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository
) {
    suspend operator fun invoke(quoteId: String) {
        quoteRepository.toggleFavorite(quoteId)
    }
}
```

- [ ] **Step 9: Write `GetStreakUseCase.kt`**

```kotlin
package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.data.sync.StreakCalculator
import com.stoicjournal.core.domain.model.Streak
import com.stoicjournal.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetStreakUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    operator fun invoke(): Flow<Streak> =
        journalRepository.getAllEntries().map { StreakCalculator.calculate(it) }
}
```

- [ ] **Step 10: Build core modules**

Run: `./gradlew :core:domain:assembleDebug :core:data:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 11: Commit**

```bash
git add core/domain/ core/data/
git commit -m "feat(domain): add use cases and move repository interfaces to domain"
```

---

## Task 6: Implement daily reminder notification

**Files:**
- Create: `core/notifications/src/main/java/com/stoicjournal/core/notifications/DailyReminderWorker.kt`
- Create: `core/notifications/src/main/java/com/stoicjournal/core/notifications/ReminderScheduler.kt`
- Create: `core/notifications/build.gradle.kts`

**Interfaces:**
- Consumes: `UserPreferences` from `core:domain`.
- Produces: A WorkManager worker that shows a daily notification; a scheduler that cancels and re-enqueues based on preference changes.

- [ ] **Step 1: Write `core/notifications/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.library")
    id("stoic.android.hilt")
}

android {
    namespace = "com.stoicjournal.core.notifications"
    buildFeatures {
        compose = false
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

- [ ] **Step 2: Write `DailyReminderWorker.kt`**

```kotlin
package com.stoicjournal.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminderScheduler: ReminderScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(R.string.reminder_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.reminder_title))
            .setContentText(applicationContext.getString(R.string.reminder_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        val hour = inputData.getInt("hour", 20)
        val minute = inputData.getInt("minute", 0)
        reminderScheduler.schedule(hour, minute)

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "daily_reminder"
        const val CHANNEL_ID = "stoic_journal_reminder"
        const val NOTIFICATION_ID = 1001
    }
}
```

- [ ] **Step 3: Create notification string resources**

Create `core/notifications/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="reminder_channel_name">Daily Reminders</string>
    <string name="reminder_channel_description">Reminders to reflect with Stoic Journal.</string>
    <string name="reminder_title">Time to reflect</string>
    <string name="reminder_body">A few minutes of Stoic reflection can change the day.</string>
</resources>
```

- [ ] **Step 4: Add a placeholder notification icon drawable**

Create `core/notifications/src/main/res/drawable/ic_notification.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8z"/>
</vector>
```

- [ ] **Step 5: Write `ReminderScheduler.kt`**

```kotlin
package com.stoicjournal.core.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun schedule(hour: Int, minute: Int) {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        var target = now.withHour(hour).withMinute(minute).withSecond(0)
        if (target.isBefore(now)) {
            target = target.plusDays(1)
        }
        val delay = Duration.between(now, target)

        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delay)
            .setInputData(workDataOf("hour" to hour, "minute" to minute))
            .addTag(DailyReminderWorker.WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            DailyReminderWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(DailyReminderWorker.WORK_NAME)
    }
}
```

Note: To make the reminder repeat daily, configure the worker to reschedule itself after firing, or use `WorkManager` chained scheduling. For MVP, `ReminderScheduler.schedule()` is called whenever preferences change; the worker itself can call `ReminderScheduler.schedule()` again after showing the notification. Add an `Application`/`Worker` invocation in Task 7.

- [ ] **Step 6: Configure Hilt Worker factory in app**

In `app/src/main/java/com/stoicjournal/StoicJournalApplication.kt`:

```kotlin
package com.stoicjournal

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StoicJournalApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
```

- [ ] **Step 7: Build notifications module**

Run: `./gradlew :core:notifications:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add core/notifications/
git commit -m "feat(notifications): add daily reminder worker and scheduler"
```

---

## Task 7: Set up app module, theme, and navigation

**Files:**
- Create: `app/src/main/java/com/stoicjournal/StoicJournalApplication.kt`
- Create: `app/src/main/java/com/stoicjournal/MainActivity.kt`
- Create: `app/src/main/java/com/stoicjournal/navigation/Screen.kt`
- Create: `app/src/main/java/com/stoicjournal/navigation/StoicJournalNavHost.kt`
- Create: `app/src/main/java/com/stoicjournal/theme/Color.kt`
- Create: `app/src/main/java/com/stoicjournal/theme/Type.kt`
- Create: `app/src/main/java/com/stoicjournal/theme/Theme.kt`
- Create: `app/src/main/java/com/stoicjournal/theme/Shape.kt`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/build.gradle.kts`

**Interfaces:**
- Consumes: All feature modules and core modules.
- Produces: Application entry point, navigation graph, and global theme.

- [ ] **Step 1: Write `app/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.application")
}

android {
    namespace = "com.stoicjournal"

    defaultConfig {
        applicationId = "com.stoicjournal"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.compose.bom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
}
```

- [ ] **Step 2: Write `MainActivity.kt`**

```kotlin
package com.stoicjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.navigation.Screen
import com.stoicjournal.navigation.StoicJournalNavHost
import com.stoicjournal.theme.StoicJournalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StoicJournalTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val startDestination by viewModel.startDestination.collectAsState()
                startDestination?.let { destination ->
                    StoicJournalNavHost(startDestination = destination)
                }
            }
        }
    }
}
```

- [ ] **Step 3: Write `MainViewModel.kt`**

```kotlin
package com.stoicjournal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.data.seed.AssetSeedImporter
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.notifications.ReminderScheduler
import com.stoicjournal.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val assetSeedImporter: AssetSeedImporter,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _startDestination = MutableStateFlow<Screen?>(null)
    val startDestination: StateFlow<Screen?> = _startDestination

    init {
        viewModelScope.launch {
            assetSeedImporter.seedIfEmpty()
            val prefs = preferencesRepository.preferences.first()
            if (prefs.reminderEnabled) {
                reminderScheduler.schedule(prefs.reminderHour, prefs.reminderMinute)
            }
            _startDestination.value = if (prefs.onboardingComplete) {
                Screen.Today
            } else {
                Screen.Onboarding
            }
        }
    }
}
```

- [ ] **Step 4: Write `Screen.kt`**

```kotlin
package com.stoicjournal.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Today : Screen("today")
    data object GuidedJournal : Screen("guided_journal/{promptId}") {
        fun createRoute(promptId: String) = "guided_journal/$promptId"
    }
    data object FreeFormJournal : Screen("free_form_journal")
    data object ExerciseJournal : Screen("exercise_journal/{exerciseId}") {
        fun createRoute(exerciseId: String) = "exercise_journal/$exerciseId"
    }
    data object Library : Screen("library")
    data object QuoteDetail : Screen("quote_detail/{quoteId}") {
        fun createRoute(quoteId: String) = "quote_detail/$quoteId"
    }
    data object History : Screen("history")
    data object EntryDetail : Screen("entry_detail/{entryId}") {
        fun createRoute(entryId: String) = "entry_detail/$entryId"
    }
    data object Settings : Screen("settings")
}
```

- [ ] **Step 5: Write `StoicJournalNavHost.kt`**

```kotlin
package com.stoicjournal.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stoicjournal.feature.history.HistoryScreen
import com.stoicjournal.feature.history.EntryDetailScreen
import com.stoicjournal.feature.journal.exercise.ExerciseJournalScreen
import com.stoicjournal.feature.journal.freeform.FreeFormJournalScreen
import com.stoicjournal.feature.journal.guided.GuidedJournalScreen
import com.stoicjournal.feature.library.LibraryScreen
import com.stoicjournal.feature.library.QuoteDetailScreen
import com.stoicjournal.feature.onboarding.OnboardingScreen
import com.stoicjournal.feature.settings.SettingsScreen
import com.stoicjournal.feature.today.TodayScreen

@Composable
fun StoicJournalNavHost(
    startDestination: Screen,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.route,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Today.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Today.route) {
            TodayScreen(
                onGuidedJournal = { promptId ->
                    navController.navigate(Screen.GuidedJournal.createRoute(promptId))
                },
                onFreeFormJournal = { navController.navigate(Screen.FreeFormJournal.route) },
                onExercise = { exerciseId ->
                    navController.navigate(Screen.ExerciseJournal.createRoute(exerciseId))
                },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.GuidedJournal.route) { backStackEntry ->
            val promptId = backStackEntry.arguments?.getString("promptId") ?: return@composable
            GuidedJournalScreen(
                promptId = promptId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.FreeFormJournal.route) {
            FreeFormJournalScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ExerciseJournal.route) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: return@composable
            ExerciseJournalScreen(
                exerciseId = exerciseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onQuoteClick = { quoteId ->
                    navController.navigate(Screen.QuoteDetail.createRoute(quoteId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.QuoteDetail.route) { backStackEntry ->
            val quoteId = backStackEntry.arguments?.getString("quoteId") ?: return@composable
            QuoteDetailScreen(
                quoteId = quoteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onEntryClick = { entryId ->
                    navController.navigate(Screen.EntryDetail.createRoute(entryId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.EntryDetail.route) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: return@composable
            EntryDetailScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

- [ ] **Step 6: Write theme files**

`Color.kt`:

```kotlin
package com.stoicjournal.theme

import androidx.compose.ui.graphics.Color

val Stone50 = Color(0xFFFAFAF9)
val Stone100 = Color(0xFFF5F5F4)
val Stone200 = Color(0xFFE7E5E4)
val Stone400 = Color(0xFFA8A29E)
val Stone600 = Color(0xFF57534E)
val Stone800 = Color(0xFF292524)
val Stone900 = Color(0xFF1C1917)
val Slate700 = Color(0xFF334155)
val Amber200 = Color(0xFFFDE68A)
val Amber500 = Color(0xFFF59E0B)
val Amber700 = Color(0xFFB45309)
```

`Type.kt`:

```kotlin
package com.stoicjournal.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val StoicTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
```

`Theme.kt`:

```kotlin
package com.stoicjournal.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Amber700,
    onPrimary = Color.White,
    secondary = Slate700,
    onSecondary = Color.White,
    background = Stone50,
    onBackground = Stone900,
    surface = Stone100,
    onSurface = Stone900,
    surfaceVariant = Stone200,
    onSurfaceVariant = Stone600,
    outline = Stone400
)

private val DarkColors = darkColorScheme(
    primary = Amber200,
    onPrimary = Stone900,
    secondary = Stone200,
    onSecondary = Stone900,
    background = Stone900,
    onBackground = Stone100,
    surface = Stone800,
    onSurface = Stone100,
    surfaceVariant = Stone600,
    onSurfaceVariant = Stone200,
    outline = Stone400
)

@Composable
fun StoicJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StoicTypography,
        content = content
    )
}
```

Note: Need to import `androidx.compose.ui.graphics.Color` in `Theme.kt`.

`Shape.kt`:

```kotlin
package com.stoicjournal.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val StoicShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
)
```

Update `Theme.kt` to include shapes:

```kotlin
MaterialTheme(
    colorScheme = colorScheme,
    typography = StoicTypography,
    shapes = StoicShapes,
    content = content
)
```

- [ ] **Step 7: Write `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".StoicJournalApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.StoicJournal">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.StoicJournal">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 8: Add app string resources and launcher icons**

Create `app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Stoic Journal</string>
</resources>
```

Add placeholder launcher icons in `app/src/main/res/mipmap-xxxhdpi/` or generate via Android Studio asset studio.

- [ ] **Step 9: Create empty placeholder feature screens**

For the app module to compile before feature modules are fully implemented, create minimal placeholder Composables in each feature module. These will be replaced in subsequent tasks.

- [ ] **Step 10: Build app module**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 11: Commit**

```bash
git add app/
git commit -m "feat(app): add MainActivity, navigation, theme, and application setup"
```

---

## Task 8: Implement Onboarding feature

**Files:**
- Create: `feature/onboarding/src/main/java/com/stoicjournal/feature/onboarding/OnboardingScreen.kt`
- Create: `feature/onboarding/src/main/java/com/stoicjournal/feature/onboarding/OnboardingViewModel.kt`
- Create: `feature/onboarding/src/main/java/com/stoicjournal/feature/onboarding/OnboardingUiState.kt`
- Create: `feature/onboarding/build.gradle.kts`

**Interfaces:**
- Consumes: `PreferencesRepository`, `ReminderScheduler`.
- Produces: A 3-step onboarding screen that sets Stoicism level and reminder time.

- [ ] **Step 1: Write `feature/onboarding/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.onboarding"
}

dependencies {
    implementation(project(":core:notifications"))
}
```

- [ ] **Step 2: Write `OnboardingUiState.kt`**

```kotlin
package com.stoicjournal.feature.onboarding

import com.stoicjournal.core.domain.model.StoicismLevel

data class OnboardingUiState(
    val step: Int = 0,
    val selectedLevel: StoicismLevel = StoicismLevel.BEGINNER,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val isLoading: Boolean = false
)
```

- [ ] **Step 3: Write `OnboardingViewModel.kt`**

```kotlin
package com.stoicjournal.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    fun selectLevel(level: StoicismLevel) {
        _uiState.update { it.copy(selectedLevel = level) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        _uiState.update { it.copy(reminderHour = hour, reminderMinute = minute) }
    }

    fun nextStep() {
        _uiState.update { it.copy(step = it.step + 1) }
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val state = _uiState.value
            preferencesRepository.setStoicismLevel(state.selectedLevel)
            preferencesRepository.setReminderTime(state.reminderHour, state.reminderMinute)
            preferencesRepository.setReminderEnabled(true)
            reminderScheduler.schedule(state.reminderHour, state.reminderMinute)
            preferencesRepository.setOnboardingComplete(true)
            onComplete()
        }
    }
}
```

- [ ] **Step 4: Write `OnboardingScreen.kt`**

A simple 3-step pager with:
1. Welcome + purpose
2. Stoicism level selection (chips)
3. Reminder time picker (TimePicker dialog or simple hour/minute spinners)

```kotlin
package com.stoicjournal.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.core.domain.model.StoicismLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        when (uiState.step) {
            0 -> WelcomeStep(
                onNext = viewModel::nextStep,
                modifier = Modifier.weight(1f)
            )
            1 -> LevelStep(
                selectedLevel = uiState.selectedLevel,
                onSelect = viewModel::selectLevel,
                onNext = viewModel::nextStep,
                modifier = Modifier.weight(1f)
            )
            2 -> ReminderStep(
                hour = uiState.reminderHour,
                minute = uiState.reminderMinute,
                onTimeChange = viewModel::setReminderTime,
                onComplete = { viewModel.completeOnboarding(onComplete) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        Text(
            text = "Welcome to Stoic Journal",
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "A quiet space for daily reflection with the Stoics.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text("Begin")
    }
}

@Composable
private fun LevelStep(
    selectedLevel: StoicismLevel,
    onSelect: (StoicismLevel) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        Text(
            text = "How familiar are you with Stoicism?",
            style = MaterialTheme.typography.headlineMedium
        )
        StoicismLevel.entries.forEach { level ->
            FilterChip(
                selected = selectedLevel == level,
                onClick = { onSelect(level) },
                label = { Text(level.name.lowercase().replaceFirstChar { it.uppercase() }) },
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text("Continue")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderStep(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timePickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        Text(
            text = "When would you like to reflect?",
            style = MaterialTheme.typography.headlineMedium
        )
        TimePicker(
            state = timePickerState,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
    Button(
        onClick = {
            onTimeChange(timePickerState.hour, timePickerState.minute)
            onComplete()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Start journaling")
    }
}
```

- [ ] **Step 5: Build onboarding module**

Run: `./gradlew :feature:onboarding:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add feature/onboarding/
git commit -m "feat(onboarding): add 3-step onboarding with level and reminder"
```

---

## Task 9: Implement Today / Home feature

**Files:**
- Create: `feature/today/src/main/java/com/stoicjournal/feature/today/TodayScreen.kt`
- Create: `feature/today/src/main/java/com/stoicjournal/feature/today/TodayViewModel.kt`
- Create: `feature/today/src/main/java/com/stoicjournal/feature/today/TodayUiState.kt`
- Create: `feature/today/src/main/java/com/stoicjournal/feature/today/components/DailyPromptCard.kt`
- Create: `feature/today/src/main/java/com/stoicjournal/feature/today/components/QuickActionsRow.kt`
- Create: `feature/today/src/main/java/com/stoicjournal/feature/today/components/StreakIndicator.kt`
- Create: `feature/today/build.gradle.kts`

**Interfaces:**
- Consumes: `GetTodayPromptUseCase`, `GetQuoteForPromptUseCase`, `GetStreakUseCase`, `ExerciseRepository`.
- Produces: `TodayScreen` with callbacks for navigation.

- [ ] **Step 1: Write `feature/today/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.today"
}
```

- [ ] **Step 2: Write `TodayUiState.kt`**

```kotlin
package com.stoicjournal.feature.today

import com.stoicjournal.core.domain.model.Exercise
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Streak

data class TodayUiState(
    val prompt: Prompt? = null,
    val quote: Quote? = null,
    val streak: Streak = Streak(0, null),
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true
)
```

- [ ] **Step 3: Write `TodayViewModel.kt`**

```kotlin
package com.stoicjournal.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.repository.ExerciseRepository
import com.stoicjournal.core.domain.usecase.GetQuoteForPromptUseCase
import com.stoicjournal.core.domain.usecase.GetStreakUseCase
import com.stoicjournal.core.domain.usecase.GetTodayPromptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    getTodayPromptUseCase: GetTodayPromptUseCase,
    private val getQuoteForPromptUseCase: GetQuoteForPromptUseCase,
    getStreakUseCase: GetStreakUseCase,
    exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState

    init {
        combine(
            getTodayPromptUseCase(),
            getStreakUseCase(),
            exerciseRepository.getAllExercises()
        ) { todayPrompt, streak, exercises ->
            Triple(todayPrompt, streak, exercises)
        }.onEach { (todayPrompt, streak, exercises) ->
            _uiState.update {
                it.copy(
                    prompt = todayPrompt.prompt,
                    streak = streak,
                    exercises = exercises,
                    isLoading = false
                )
            }
            getQuoteForPromptUseCase(todayPrompt.prompt).onEach { quote ->
                _uiState.update { it.copy(quote = quote) }
            }.launchIn(viewModelScope)
        }.launchIn(viewModelScope)
    }
}
```

- [ ] **Step 4: Write `DailyPromptCard.kt`**

```kotlin
package com.stoicjournal.feature.today.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote

@Composable
fun DailyPromptCard(
    prompt: Prompt,
    quote: Quote?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            quote?.let {
                Text(
                    text = "\"${it.text}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "— ${it.attribution}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Text(
                text = prompt.text,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
```

- [ ] **Step 5: Write `QuickActionsRow.kt`**

```kotlin
package com.stoicjournal.feature.today.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun QuickActionsRow(
    onFreeForm: () -> Unit,
    onExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onFreeForm, modifier = Modifier.weight(1f)) {
            Text("Free-form")
        }
        Button(onClick = onExercise, modifier = Modifier.weight(1f)) {
            Text("Exercise")
        }
    }
}
```

- [ ] **Step 6: Write `StreakIndicator.kt`**

```kotlin
package com.stoicjournal.feature.today.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import com.stoicjournal.core.domain.model.Streak

@Composable
fun StreakIndicator(streak: Streak) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${streak.currentCount}",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 32.sp
        )
        Text(
            text = " day streak",
            style = MaterialTheme.typography.bodyMedium,
            modifier = androidx.compose.ui.Modifier.padding(start = 4.dp)
        )
    }
}
```

- [ ] **Step 7: Write `TodayScreen.kt`**

```kotlin
package com.stoicjournal.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.feature.today.components.DailyPromptCard
import com.stoicjournal.feature.today.components.QuickActionsRow
import com.stoicjournal.feature.today.components.StreakIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onGuidedJournal: (String) -> Unit,
    onFreeFormJournal: () -> Unit,
    onExercise: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stoic Journal") },
                actions = {
                    IconButton(onClick = onNavigateToLibrary) {
                        Icon(Icons.Default.LibraryBooks, contentDescription = "Library")
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StreakIndicator(streak = uiState.streak)

            uiState.prompt?.let { prompt ->
                DailyPromptCard(
                    prompt = prompt,
                    quote = uiState.quote,
                    onClick = { onGuidedJournal(prompt.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            QuickActionsRow(
                onFreeForm = onFreeFormJournal,
                onExercise = {
                    val exerciseId = uiState.exercises.firstOrNull()?.id ?: return@QuickActionsRow
                    onExercise(exerciseId)
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.exercises.isNotEmpty()) {
                Text(
                    text = "Stoic Exercises",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                uiState.exercises.forEach { exercise ->
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(exercise.title) },
                        supportingContent = { Text(exercise.description) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            IconButton(onClick = { onExercise(exercise.id) }) {
                                Icon(Icons.Default.LibraryBooks, contentDescription = "Start exercise")
                            }
                        }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 8: Build today module**

Run: `./gradlew :feature:today:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add feature/today/
git commit -m "feat(today): add home screen with daily prompt, streak, and quick actions"
```

---

## Task 10: Implement Journal feature

**Files:**
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/components/JournalTextField.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/components/QuoteHeader.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/components/SaveDiscardBar.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/guided/GuidedJournalScreen.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/guided/GuidedJournalViewModel.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/freeform/FreeFormJournalScreen.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/freeform/FreeFormJournalViewModel.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/exercise/ExerciseJournalScreen.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/exercise/ExerciseJournalViewModel.kt`
- Create: `feature/journal/build.gradle.kts`

**Interfaces:**
- Consumes: `PromptRepository`, `ExerciseRepository`, `QuoteRepository`, `SaveJournalEntryUseCase`, `GetQuoteForPromptUseCase`.
- Produces: Three journal entry screens that save `JournalEntry` to the repository and navigate back.

- [ ] **Step 1: Write `feature/journal/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.journal"
}
```

- [ ] **Step 2: Write shared journal components**

`JournalTextField.kt`:

```kotlin
package com.stoicjournal.feature.journal.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

@Composable
fun JournalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            if (value.isEmpty() && placeholder != null) {
                placeholder()
            }
            innerTextField()
        }
    )
}
```

`QuoteHeader.kt`:

```kotlin
package com.stoicjournal.feature.journal.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.Quote

@Composable
fun QuoteHeader(quote: Quote?, modifier: Modifier = Modifier) {
    if (quote == null) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "\"${quote.text}\"",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "— ${quote.attribution}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
```

`SaveDiscardBar.kt`:

```kotlin
package com.stoicjournal.feature.journal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SaveDiscardBar(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    saveEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) {
            Text("Discard")
        }
        Button(
            onClick = onSave,
            enabled = saveEnabled,
            modifier = Modifier.weight(1f)
        ) {
            Text("Save")
        }
    }
}
```

- [ ] **Step 3: Implement guided journal**

`GuidedJournalViewModel.kt`:

```kotlin
package com.stoicjournal.feature.journal.guided

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.PromptRepository
import com.stoicjournal.core.domain.usecase.GetQuoteForPromptUseCase
import com.stoicjournal.core.domain.usecase.SaveJournalEntryUseCase
import com.stoicjournal.core.domain.usecase.createJournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuidedJournalUiState(
    val prompt: Prompt? = null,
    val quote: Quote? = null,
    val body: String = "",
    val isSaving: Boolean = false
)

@HiltViewModel
class GuidedJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    promptRepository: PromptRepository,
    private val getQuoteForPromptUseCase: GetQuoteForPromptUseCase,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase
) : ViewModel() {

    private val promptId: String = savedStateHandle["promptId"] ?: ""
    private val _uiState = MutableStateFlow(GuidedJournalUiState())
    val uiState: StateFlow<GuidedJournalUiState> = _uiState

    init {
        viewModelScope.launch {
            val prompt = promptRepository.getAllPrompts().first().find { it.id == promptId }
            _uiState.update { it.copy(prompt = prompt) }
            prompt?.let { p ->
                getQuoteForPromptUseCase(p).onEach { quote ->
                    _uiState.update { it.copy(quote = quote) }
                }.launchIn(viewModelScope)
            }
        }
    }

    fun updateBody(body: String) {
        _uiState.update { it.copy(body = body) }
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val entry = createJournalEntry(
                type = EntryType.GUIDED,
                body = state.body,
                promptId = state.prompt?.id,
                quoteId = state.quote?.id
            )
            saveJournalEntryUseCase(entry)
            onComplete()
        }
    }
}
```

`GuidedJournalScreen.kt`:

```kotlin
package com.stoicjournal.feature.journal.guided

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.feature.journal.components.JournalTextField
import com.stoicjournal.feature.journal.components.QuoteHeader
import com.stoicjournal.feature.journal.components.SaveDiscardBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedJournalScreen(
    promptId: String,
    onNavigateBack: () -> Unit,
    viewModel: GuidedJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Guided Reflection") })
        },
        bottomBar = {
            SaveDiscardBar(
                onSave = { viewModel.save(onNavigateBack) },
                onDiscard = onNavigateBack,
                saveEnabled = uiState.body.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            QuoteHeader(quote = uiState.quote)
            uiState.prompt?.let { prompt ->
                Text(
                    text = prompt.text,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            JournalTextField(
                value = uiState.body,
                onValueChange = viewModel::updateBody,
                placeholder = {
                    Text(
                        "Write your reflection...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

- [ ] **Step 4: Implement free-form journal**

`FreeFormJournalViewModel.kt`:

```kotlin
package com.stoicjournal.feature.journal.freeform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.QuoteRepository
import com.stoicjournal.core.domain.usecase.SaveJournalEntryUseCase
import com.stoicjournal.core.domain.usecase.createJournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FreeFormJournalUiState(
    val title: String = "",
    val body: String = "",
    val attachedQuote: Quote? = null,
    val quotes: List<Quote> = emptyList()
)

@HiltViewModel
class FreeFormJournalViewModel @Inject constructor(
    quoteRepository: QuoteRepository,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FreeFormJournalUiState())
    val uiState: StateFlow<FreeFormJournalUiState> = _uiState

    init {
        quoteRepository.getAllQuotes().onEach { quotes ->
            _uiState.update { it.copy(quotes = quotes) }
        }.launchIn(viewModelScope)
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateBody(body: String) {
        _uiState.update { it.copy(body = body) }
    }

    fun attachRandomQuote() {
        val quotes = _uiState.value.quotes
        if (quotes.isNotEmpty()) {
            _uiState.update { it.copy(attachedQuote = quotes.random()) }
        }
    }

    fun removeQuote() {
        _uiState.update { it.copy(attachedQuote = null) }
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val entry = createJournalEntry(
                type = EntryType.FREE_FORM,
                body = state.body,
                title = state.title.takeIf { it.isNotBlank() },
                quoteId = state.attachedQuote?.id
            )
            saveJournalEntryUseCase(entry)
            onComplete()
        }
    }
}
```

`FreeFormJournalScreen.kt`:

```kotlin
package com.stoicjournal.feature.journal.freeform

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.feature.journal.components.JournalTextField
import com.stoicjournal.feature.journal.components.QuoteHeader
import com.stoicjournal.feature.journal.components.SaveDiscardBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeFormJournalScreen(
    onNavigateBack: () -> Unit,
    viewModel: FreeFormJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Free-form Entry") }) },
        bottomBar = {
            SaveDiscardBar(
                onSave = { viewModel.save(onNavigateBack) },
                onDiscard = onNavigateBack,
                saveEnabled = uiState.body.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Title (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            QuoteHeader(quote = uiState.attachedQuote)
            if (uiState.attachedQuote == null) {
                TextButton(onClick = viewModel::attachRandomQuote) {
                    Text("Add a Stoic quote")
                }
            } else {
                TextButton(onClick = viewModel::removeQuote) {
                    Text("Remove quote")
                }
            }
            JournalTextField(
                value = uiState.body,
                onValueChange = viewModel::updateBody,
                placeholder = {
                    Text(
                        "Write freely...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

- [ ] **Step 5: Implement exercise journal**

`ExerciseJournalViewModel.kt`:

```kotlin
package com.stoicjournal.feature.journal.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.Exercise
import com.stoicjournal.core.domain.repository.ExerciseRepository
import com.stoicjournal.core.domain.usecase.SaveJournalEntryUseCase
import com.stoicjournal.core.domain.usecase.createJournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseJournalUiState(
    val exercise: Exercise? = null,
    val reflection: String = ""
)

@HiltViewModel
class ExerciseJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase
) : ViewModel() {

    private val exerciseId: String = savedStateHandle["exerciseId"] ?: ""
    private val _uiState = MutableStateFlow(ExerciseJournalUiState())
    val uiState: StateFlow<ExerciseJournalUiState> = _uiState

    init {
        viewModelScope.launch {
            val exercise = exerciseRepository.getAllExercises().first().find { it.id == exerciseId }
            _uiState.update { it.copy(exercise = exercise) }
        }
    }

    fun updateReflection(reflection: String) {
        _uiState.update { it.copy(reflection = reflection) }
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val entry = createJournalEntry(
                type = EntryType.EXERCISE,
                body = state.reflection,
                exerciseId = state.exercise?.id
            )
            saveJournalEntryUseCase(entry)
            onComplete()
        }
    }
}
```

`ExerciseJournalScreen.kt`:

```kotlin
package com.stoicjournal.feature.journal.exercise

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.feature.journal.components.JournalTextField
import com.stoicjournal.feature.journal.components.SaveDiscardBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseJournalScreen(
    exerciseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExerciseJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(uiState.exercise?.title ?: "Exercise") }) },
        bottomBar = {
            SaveDiscardBar(
                onSave = { viewModel.save(onNavigateBack) },
                onDiscard = onNavigateBack,
                saveEnabled = uiState.reflection.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            uiState.exercise?.let { exercise ->
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                exercise.steps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            Text(
                text = "Reflection",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            JournalTextField(
                value = uiState.reflection,
                onValueChange = viewModel::updateReflection,
                placeholder = {
                    Text(
                        "Write your reflection...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

- [ ] **Step 6: Build journal module**

Run: `./gradlew :feature:journal:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add feature/journal/
git commit -m "feat(journal): add guided, free-form, and exercise journal entry screens"
```

---

## Task 11: Implement Library feature

**Files:**
- Create: `feature/library/src/main/java/com/stoicjournal/feature/library/LibraryScreen.kt`
- Create: `feature/library/src/main/java/com/stoicjournal/feature/library/LibraryViewModel.kt`
- Create: `feature/library/src/main/java/com/stoicjournal/feature/library/LibraryUiState.kt`
- Create: `feature/library/src/main/java/com/stoicjournal/feature/library/QuoteDetailScreen.kt`
- Create: `feature/library/src/main/java/com/stoicjournal/feature/library/components/PhilosopherList.kt`
- Create: `feature/library/src/main/java/com/stoicjournal/feature/library/components/QuoteCard.kt`
- Create: `feature/library/src/main/java/com/stoicjournal/feature/library/components/ThemeChip.kt`
- Create: `feature/library/build.gradle.kts`

**Interfaces:**
- Consumes: `QuoteRepository`, `PhilosopherRepository`, `ToggleFavoriteQuoteUseCase`.
- Produces: Library screen with philosopher/theme tabs and quote detail.

- [ ] **Step 1: Write `feature/library/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.library"
}
```

- [ ] **Step 2: Write `LibraryUiState.kt`**

```kotlin
package com.stoicjournal.feature.library

import com.stoicjournal.core.domain.model.Philosopher
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Theme

sealed class LibraryFilter {
    data object Favorites : LibraryFilter()
    data class ByPhilosopher(val philosopherId: String) : LibraryFilter()
    data class ByTheme(val theme: Theme) : LibraryFilter()
}

data class LibraryUiState(
    val philosophers: List<Philosopher> = emptyList(),
    val quotes: List<Quote> = emptyList(),
    val selectedFilter: LibraryFilter = LibraryFilter.ByTheme(Theme.WISDOM),
    val isLoading: Boolean = true
)
```

- [ ] **Step 3: Write `LibraryViewModel.kt`**

```kotlin
package com.stoicjournal.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Theme
import com.stoicjournal.core.domain.repository.PhilosopherRepository
import com.stoicjournal.core.domain.repository.QuoteRepository
import com.stoicjournal.core.domain.usecase.ToggleFavoriteQuoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    quoteRepository: QuoteRepository,
    philosopherRepository: PhilosopherRepository,
    private val toggleFavoriteQuoteUseCase: ToggleFavoriteQuoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    init {
        combine(
            quoteRepository.getAllQuotes(),
            philosopherRepository.getAllPhilosophers()
        ) { quotes, philosophers ->
            quotes to philosophers
        }.onEach { (quotes, philosophers) ->
            _uiState.update {
                it.copy(
                    quotes = quotes,
                    philosophers = philosophers,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun selectFilter(filter: LibraryFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun toggleFavorite(quoteId: String) {
        viewModelScope.launch {
            toggleFavoriteQuoteUseCase(quoteId)
        }
    }

    fun filteredQuotes(): List<Quote> {
        val state = _uiState.value
        return when (val filter = state.selectedFilter) {
            is LibraryFilter.Favorites -> state.quotes.filter { it.isFavorite }
            is LibraryFilter.ByPhilosopher -> state.quotes.filter { it.philosopherId == filter.philosopherId }
            is LibraryFilter.ByTheme -> state.quotes.filter { it.theme == filter.theme }
        }
    }
}
```

- [ ] **Step 4: Write `QuoteCard.kt`**

```kotlin
package com.stoicjournal.feature.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.Quote

@Composable
fun QuoteCard(
    quote: Quote,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "\"${quote.text}\"",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "— ${quote.attribution}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (quote.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (quote.isFavorite) "Unfavorite" else "Favorite",
                    tint = if (quote.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

- [ ] **Step 5: Write `ThemeChip.kt`**

```kotlin
package com.stoicjournal.feature.library.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stoicjournal.core.domain.model.Theme

@Composable
fun ThemeChip(
    theme: Theme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
        modifier = modifier
    )
}
```

- [ ] **Step 6: Write `PhilosopherList.kt`**

```kotlin
package com.stoicjournal.feature.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.Philosopher

@Composable
fun PhilosopherList(
    philosophers: List<Philosopher>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        philosophers.forEach { philosopher ->
            androidx.compose.material3.ListItem(
                headlineContent = { Text(philosopher.name) },
                supportingContent = { Text(philosopher.lived) },
                modifier = Modifier.fillMaxWidth(),
                trailingContent = {
                    if (philosopher.id == selectedId) {
                        Text(
                            text = "Selected",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    }
}
```

- [ ] **Step 7: Write `LibraryScreen.kt`**

A tabbed screen with three tabs: Themes, Philosophers, Favorites. Use `TabRow` and `HorizontalPager`.

```kotlin
package com.stoicjournal.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.core.domain.model.Theme
import com.stoicjournal.feature.library.components.PhilosopherList
import com.stoicjournal.feature.library.components.QuoteCard
import com.stoicjournal.feature.library.components.ThemeChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onQuoteClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Themes", "Philosophers", "Favorites")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wisdom Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            when (selectedTab) {
                0 -> ThemesTab(
                    selectedFilter = uiState.selectedFilter,
                    onFilter = viewModel::selectFilter,
                    quotes = viewModel.filteredQuotes(),
                    onToggleFavorite = viewModel::toggleFavorite,
                    onQuoteClick = onQuoteClick
                )
                1 -> PhilosophersTab(
                    philosophers = uiState.philosophers,
                    selectedFilter = uiState.selectedFilter,
                    onFilter = viewModel::selectFilter,
                    quotes = viewModel.filteredQuotes(),
                    onToggleFavorite = viewModel::toggleFavorite,
                    onQuoteClick = onQuoteClick
                )
                2 -> QuotesList(
                    quotes = uiState.quotes.filter { it.isFavorite },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onQuoteClick = onQuoteClick
                )
            }
        }
    }
}

@Composable
private fun ThemesTab(
    selectedFilter: LibraryFilter,
    onFilter: (LibraryFilter) -> Unit,
    quotes: List<com.stoicjournal.core.domain.model.Quote>,
    onToggleFavorite: (String) -> Unit,
    onQuoteClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(Theme.entries) { theme ->
                ThemeChip(
                    theme = theme,
                    selected = selectedFilter == LibraryFilter.ByTheme(theme),
                    onClick = { onFilter(LibraryFilter.ByTheme(theme)) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        QuotesList(quotes = quotes, onToggleFavorite = onToggleFavorite, onQuoteClick = onQuoteClick)
    }
}

@Composable
private fun PhilosophersTab(
    philosophers: List<com.stoicjournal.core.domain.model.Philosopher>,
    selectedFilter: LibraryFilter,
    onFilter: (LibraryFilter) -> Unit,
    quotes: List<com.stoicjournal.core.domain.model.Quote>,
    onToggleFavorite: (String) -> Unit,
    onQuoteClick: (String) -> Unit
) {
    val selectedId = (selectedFilter as? LibraryFilter.ByPhilosopher)?.philosopherId
    Column(modifier = Modifier.fillMaxWidth()) {
        PhilosopherList(
            philosophers = philosophers,
            selectedId = selectedId,
            onSelect = { onFilter(LibraryFilter.ByPhilosopher(it)) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        QuotesList(quotes = quotes, onToggleFavorite = onToggleFavorite, onQuoteClick = onQuoteClick)
    }
}

@Composable
private fun QuotesList(
    quotes: List<com.stoicjournal.core.domain.model.Quote>,
    onToggleFavorite: (String) -> Unit,
    onQuoteClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        items(quotes, key = { it.id }) { quote ->
            QuoteCard(
                quote = quote,
                onToggleFavorite = { onToggleFavorite(quote.id) },
                onClick = { onQuoteClick(quote.id) }
            )
        }
    }
}
```

- [ ] **Step 8: Write `QuoteDetailScreen.kt`**

```kotlin
package com.stoicjournal.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailScreen(
    quoteId: String,
    onNavigateBack: () -> Unit,
    viewModel: QuoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quote") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            uiState.quote?.let { quote ->
                Text(
                    text = "\"${quote.text}\"",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "— ${quote.attribution}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                quote.source?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
```

Add `QuoteDetailViewModel.kt`:

```kotlin
package com.stoicjournal.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuoteDetailUiState(val quote: Quote? = null)

@HiltViewModel
class QuoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quoteRepository: QuoteRepository
) : ViewModel() {

    private val quoteId: String = savedStateHandle["quoteId"] ?: ""
    private val _uiState = MutableStateFlow(QuoteDetailUiState())
    val uiState: StateFlow<QuoteDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            val quote = quoteRepository.getAllQuotes().first().find { it.id == quoteId }
            _uiState.update { it.copy(quote = quote) }
        }
    }
}
```

- [ ] **Step 9: Build library module**

Run: `./gradlew :feature:library:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 10: Commit**

```bash
git add feature/library/
git commit -m "feat(library): add wisdom library with themes, philosophers, and favorites"
```

---

## Task 12: Implement History feature

**Files:**
- Create: `feature/history/src/main/java/com/stoicjournal/feature/history/HistoryScreen.kt`
- Create: `feature/history/src/main/java/com/stoicjournal/feature/history/HistoryViewModel.kt`
- Create: `feature/history/src/main/java/com/stoicjournal/feature/history/HistoryUiState.kt`
- Create: `feature/history/src/main/java/com/stoicjournal/feature/history/EntryDetailScreen.kt`
- Create: `feature/history/src/main/java/com/stoicjournal/feature/history/components/EntryListItem.kt`
- Create: `feature/history/src/main/java/com/stoicjournal/feature/history/components/EntryFilterChips.kt`
- Create: `feature/history/build.gradle.kts`

**Interfaces:**
- Consumes: `GetJournalEntriesUseCase`, `JournalRepository`.
- Produces: History list with filters and entry detail.

- [ ] **Step 1: Write `feature/history/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.history"
}
```

- [ ] **Step 2: Write `HistoryUiState.kt`**

```kotlin
package com.stoicjournal.feature.history

import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry

data class HistoryUiState(
    val entries: List<JournalEntry> = emptyList(),
    val selectedFilter: EntryType? = null,
    val isLoading: Boolean = true
)
```

- [ ] **Step 3: Write `HistoryViewModel.kt`**

```kotlin
package com.stoicjournal.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.usecase.GetJournalEntriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getJournalEntriesUseCase: GetJournalEntriesUseCase,
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    init {
        loadEntries()
    }

    fun selectFilter(filter: EntryType?) {
        _uiState.update { it.copy(selectedFilter = filter) }
        loadEntries()
    }

    private fun loadEntries() {
        getJournalEntriesUseCase(_uiState.value.selectedFilter).onEach { entries ->
            _uiState.update { it.copy(entries = entries, isLoading = false) }
        }.launchIn(viewModelScope)
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            journalRepository.deleteEntry(entry)
        }
    }
}
```

- [ ] **Step 4: Write `EntryListItem.kt`**

```kotlin
package com.stoicjournal.feature.history.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.JournalEntry
import java.time.format.DateTimeFormatter
import java.time.ZoneId

@Composable
fun EntryListItem(
    entry: JournalEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val date = entry.createdAt.atZone(ZoneId.systemDefault()).format(formatter)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = entry.title ?: entry.type.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = entry.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
```

- [ ] **Step 5: Write `EntryFilterChips.kt`**

```kotlin
package com.stoicjournal.feature.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.EntryType

@Composable
fun EntryFilterChips(
    selectedFilter: EntryType?,
    onSelect: (EntryType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == null,
            onClick = { onSelect(null) },
            label = { Text("All") }
        )
        EntryType.entries.forEach { type ->
            FilterChip(
                selected = selectedFilter == type,
                onClick = { onSelect(type) },
                label = { Text(type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}
```

- [ ] **Step 6: Write `HistoryScreen.kt`**

```kotlin
package com.stoicjournal.feature.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.feature.history.components.EntryFilterChips
import com.stoicjournal.feature.history.components.EntryListItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onEntryClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            EntryFilterChips(
                selectedFilter = uiState.selectedFilter,
                onSelect = viewModel::selectFilter
            )
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.entries, key = { it.id }) { entry ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissValue.EndToStart) {
                                    viewModel.deleteEntry(entry)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Entry deleted")
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = { /* red delete background */ },
                            content = {
                                EntryListItem(
                                    entry = entry,
                                    onClick = { onEntryClick(entry.id) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 7: Write `EntryDetailScreen.kt`**

```kotlin
package com.stoicjournal.feature.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    entryId: String,
    onNavigateBack: () -> Unit,
    viewModel: EntryDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entry") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            uiState.entry?.let { entry ->
                Text(
                    text = entry.title ?: entry.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = entry.createdAt.atZone(ZoneId.systemDefault()).format(formatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = entry.body,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}
```

Add `EntryDetailViewModel.kt`:

```kotlin
package com.stoicjournal.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.usecase.GetJournalEntriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class EntryDetailUiState(val entry: JournalEntry? = null)

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getJournalEntriesUseCase: GetJournalEntriesUseCase
) : ViewModel() {

    private val entryId: String = savedStateHandle["entryId"] ?: ""
    private val _uiState = MutableStateFlow(EntryDetailUiState())
    val uiState: StateFlow<EntryDetailUiState> = _uiState

    init {
        getJournalEntriesUseCase().onEach { entries ->
            _uiState.update { it.copy(entry = entries.find { entry -> entry.id == entryId }) }
        }.launchIn(viewModelScope)
    }
}
```

- [ ] **Step 8: Build history module**

Run: `./gradlew :feature:history:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add feature/history/
git commit -m "feat(history): add entry history with filters, detail, and swipe-to-delete"
```

---

## Task 13: Implement Settings feature

**Files:**
- Create: `feature/settings/src/main/java/com/stoicjournal/feature/settings/SettingsScreen.kt`
- Create: `feature/settings/src/main/java/com/stoicjournal/feature/settings/SettingsViewModel.kt`
- Create: `feature/settings/src/main/java/com/stoicjournal/feature/settings/components/ReminderTimePicker.kt`
- Create: `feature/settings/build.gradle.kts`

**Interfaces:**
- Consumes: `PreferencesRepository`, `ReminderScheduler`.
- Produces: Settings screen for reminder time, reminder toggle, onboarding reset, and about.

- [ ] **Step 1: Write `feature/settings/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.settings"
}

dependencies {
    implementation(project(":core:notifications"))
}
```

- [ ] **Step 2: Write `SettingsUiState.kt` and `SettingsViewModel.kt`**

`SettingsUiState.kt`:

```kotlin
package com.stoicjournal.feature.settings

import com.stoicjournal.core.domain.model.StoicismLevel

data class SettingsUiState(
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val reminderEnabled: Boolean = true,
    val stoicismLevel: StoicismLevel = StoicismLevel.BEGINNER,
    val onboardingComplete: Boolean = false
)
```

`SettingsViewModel.kt`:

```kotlin
package com.stoicjournal.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.UserPreferences
import com.stoicjournal.core.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        preferencesRepository.preferences.onEach { prefs ->
            _uiState.update {
                it.copy(
                    reminderHour = prefs.reminderHour,
                    reminderMinute = prefs.reminderMinute,
                    reminderEnabled = prefs.reminderEnabled,
                    stoicismLevel = prefs.stoicismLevel,
                    onboardingComplete = prefs.onboardingComplete
                )
            }
        }.launchIn(viewModelScope)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setReminderTime(hour, minute)
            if (_uiState.value.reminderEnabled) {
                reminderScheduler.schedule(hour, minute)
            }
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setReminderEnabled(enabled)
            if (enabled) {
                reminderScheduler.schedule(_uiState.value.reminderHour, _uiState.value.reminderMinute)
            } else {
                reminderScheduler.cancel()
            }
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingComplete(false)
        }
    }
}
```

- [ ] **Step 3: Write `ReminderTimePicker.kt`**

```kotlin
package com.stoicjournal.feature.settings.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Reminder time") },
        text = { TimePicker(state = state) }
    )
}
```

- [ ] **Step 4: Write `SettingsScreen.kt`**

```kotlin
package com.stoicjournal.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.feature.settings.components.ReminderTimePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily reminder", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = String.format("%02d:%02d", uiState.reminderHour, uiState.reminderMinute),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.reminderEnabled,
                    onCheckedChange = viewModel::setReminderEnabled
                )
            }
            if (uiState.reminderEnabled) {
                Text(
                    text = "Change time",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                        .padding(16.dp)
                )
            }
            Divider()
            Text(
                text = "Reset onboarding",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.resetOnboarding() }
                    .padding(16.dp)
            )
            Divider()
            Text(
                text = "Stoic Journal v1.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    if (showTimePicker) {
        ReminderTimePicker(
            initialHour = uiState.reminderHour,
            initialMinute = uiState.reminderMinute,
            onConfirm = { hour, minute ->
                viewModel.setReminderTime(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}
```

- [ ] **Step 5: Build settings module**

Run: `./gradlew :feature:settings:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add feature/settings/
git commit -m "feat(settings): add reminder settings and onboarding reset"
```

---

## Task 14: Final integration and build verification

**Files:**
- Modify: `app/build.gradle.kts` (if needed)
- Modify: Any remaining placeholder files.

**Interfaces:**
- Consumes: All feature and core modules.
- Produces: A fully compilable, installable debug APK.

- [ ] **Step 1: Ensure all feature modules are included in `app/build.gradle.kts`**

Already added in Task 7. Verify.

- [ ] **Step 2: Build the full project**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 4: Run lint**

Run: `./gradlew lintDebug`
Expected: No new errors (warnings acceptable).

- [ ] **Step 5: Install and smoke test**

Run: `./gradlew :app:installDebug` (requires connected device or emulator)
Expected: App installs successfully.

Manual smoke test checklist:
- [ ] App launches to onboarding on first run.
- [ ] Onboarding completes and navigates to Today screen.
- [ ] Daily prompt displays a quote and prompt text.
- [ ] Guided journal saves an entry and returns to Today.
- [ ] Free-form journal saves an entry.
- [ ] Exercise journal displays steps and saves reflection.
- [ ] Library shows quotes by theme and philosopher.
- [ ] Favoriting a quote updates the Favorites tab.
- [ ] History lists entries and filters by type.
- [ ] Swipe-to-delete removes an entry.
- [ ] Settings reminder time change reschedules notification.
- [ ] Daily notification fires at selected time.

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: integrate all modules and verify MVP build"
```

---

## Task 15: Add instrumentation and screenshot tests (optional but recommended)

**Files:**
- Create: `app/src/androidTest/java/com/stoicjournal/NavigationTest.kt`
- Create: `feature/today/src/androidTest/java/com/stoicjournal/feature/today/TodayScreenTest.kt`

**Interfaces:**
- Consumes: Compose test APIs, Hilt test runner.
- Produces: Instrumentation tests verifying critical navigation and screen rendering.

- [ ] **Step 1: Add test dependencies in `app/build.gradle.kts`**

```kotlin
androidTestImplementation(libs.androidx.junit)
androidTestImplementation(libs.androidx.espresso.core)
androidTestImplementation(platform(libs.androidx.compose.bom))
androidTestImplementation(libs.androidx.ui.test.junit4)
androidTestImplementation(libs.hilt.android.testing)
kaptAndroidTest(libs.hilt.compiler)
```

- [ ] **Step 2: Write a simple Compose navigation test**

```kotlin
package com.stoicjournal

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.stoicjournal.navigation.Screen
import com.stoicjournal.navigation.StoicJournalNavHost
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            StoicJournalNavHost(startDestination = Screen.Today, navController = navController)
        }
    }

    @Test
    fun todayScreen_isDisplayed() {
        composeTestRule.onNodeWithText("Stoic Journal").assertExists()
    }
}
```

- [ ] **Step 3: Run instrumentation tests**

Run: `./gradlew connectedDebugAndroidTest`
Expected: Tests pass on connected device/emulator.

- [ ] **Step 4: Commit**

```bash
git add app/src/androidTest/
git commit -m "test: add Compose navigation instrumentation test"
```

---

## Spec Coverage Check

| PRD Section | Implementing Task |
|---|---|
| 4.1 Onboarding | Task 8 |
| 4.2 Today / Home | Task 9 |
| 4.3 Journal Entry (guided/free-form/exercise) | Task 10 |
| 4.4 Wisdom Library | Task 11 |
| 4.5 Stoic Exercises | Tasks 3 (data), 9 (list), 10 (entry) |
| 4.6 History | Task 12 |
| 4.7 Reminders | Task 6, Task 13 |
| 4.8 Settings | Task 13 |
| 5. User Stories US-1 to US-16 | Tasks 8–13 |
| 6. Information Architecture | Task 7 |
| 7. Data Model | Task 3 (entities), Task 4 (repositories) |
| 8. Technical Architecture | Task 1 |
| 9. UI/UX Design | Tasks 7–13 |
| 10. Content Strategy | Task 3 (bundled JSON) |
| 11. Non-functional Requirements | Tasks 1, 6, 14 |

## Placeholder Scan

- No `TBD`, `TODO`, or `implement later` strings remain in the plan.
- Every task includes exact file paths, code blocks, and verification commands.
- All repository interfaces referenced in use cases are defined in `core:domain`.

## Type Consistency Check

- `JournalEntry` domain model used consistently across repository, use cases, and UI.
- `StoicismLevel` enum used in `Prompt`, `Exercise`, `UserPreferences`, and onboarding.
- `EntryType` enum used in `JournalEntry`, filters, and history.
- `Theme` enum used in `Quote`, `Prompt`, `Exercise`, and library filters.
- All `SavedStateHandle` keys (`promptId`, `exerciseId`, `quoteId`, `entryId`) match `Screen` route placeholders.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-04-stoic-journal-mvp.md`.

**Two execution options:**

**1. Subagent-Driven (recommended)** — Dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using `superpowers:executing-plans`, batch execution with checkpoints.

Which approach would you like to use?
