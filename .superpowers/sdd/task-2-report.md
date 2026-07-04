# Task 2 Report: Create core domain models

## What was implemented

Created the `:core:domain` Android library module and the domain model classes specified in the task brief:

- `core/domain/build.gradle.kts` — applies `stoic.android.library`, disables Compose, and adds Hilt dependencies.
- `core/domain/src/main/java/com/stoicjournal/core/domain/model/EntryType.kt`
- `core/domain/src/main/java/com/stoicjournal/core/domain/model/StoicismLevel.kt`
- `core/domain/src/main/java/com/stoicjournal/core/domain/model/Theme.kt`
- `core/domain/src/main/java/com/stoicjournal/core/domain/model/JournalEntry.kt`
- `core/domain/src/main/java/com/stoicjournal/core/domain/model/Quote.kt`
- `core/domain/src/main/java/com/stoicjournal/core/domain/model/Prompt.kt`
- `core/domain/src/main/java/com/stoicjournal/core/domain/model/Exercise.kt`
- `core/domain/src/main/java/com/stoicjournal/core/domain/model/Philosopher.kt`
- `core/domain/src/main/java/com/stoicjournal/core/domain/model/Streak.kt`

The models are plain Kotlin `data class` and `enum class` definitions, using `java.time.Instant` and `java.time.LocalDate` where required.

## Build-logic fixes required

The `:core:domain` module could not compile because the `build-logic/convention` plugins from Task 1 had compilation errors:

- Missing imports for `JavaVersion` and Kotlin 2.0-compatible JVM target configuration.
- `dependencies { implementation(...) }` DSL was not valid inside class-based convention plugins.
- Version catalog `libs` was not accessible in class-based plugins.

To unblock this task, the following convention plugin files were updated:

- `build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt`
- `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt`
- `build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt`
- `build-logic/convention/src/main/kotlin/AndroidHiltConventionPlugin.kt`
- `build-logic/convention/src/main/kotlin/AndroidRoomConventionPlugin.kt`

Changes included:

- Applying `org.jetbrains.kotlin.kapt` in `AndroidLibraryConventionPlugin` so that `:core:domain` can use the `kapt` configuration.
- Using `KotlinAndroidProjectExtension.compilerOptions` with `JvmTarget.JVM_17` instead of the removed `kotlinOptions` block.
- Accessing the version catalog via `VersionCatalogsExtension` and adding dependencies with `project.dependencies.apply { add(...) }`.

## What was tested

Ran the verification command from the task brief:

```bash
./gradlew :core:domain:assembleDebug
```

Result: **BUILD SUCCESSFUL**.

A warning is emitted: `Kapt currently doesn't support language version 2.0+. Falling back to 1.9.` This does not break the build.

## Files changed

- `.gitignore` — added `**/build/` so nested module build outputs are ignored.
- `core/domain/build.gradle.kts`
- `core/domain/src/main/java/com/stoicjournal/core/domain/model/*.kt` (10 files)
- `build-logic/convention/src/main/kotlin/Android*.kt` (5 convention plugin files)

## Commits

- `fb46a2e` — `build(build-logic): fix convention plugin compilation`
- `50ccc0e` — `feat(domain): add core domain models`

## Self-review findings

- All requested model classes are present and match the brief exactly.
- The domain module compiles cleanly.
- No test sources were required by the brief.
- Build outputs were accidentally committed on the first attempt; they were removed and `.gitignore` was updated to prevent recurrence.

## Issues / concerns

- I had to modify build-logic convention plugins outside the strict scope of Task 2 to make `:core:domain` compile. These fixes were necessary because Task 1's convention plugins did not compile against Kotlin 2.0 and the version catalog.
- The `kapt` warning should be monitored; future tasks may want to migrate to KSP if Room/Hilt support it.
- I added `apply("org.jetbrains.kotlin.kapt")` inside `AndroidLibraryConventionPlugin` because `:core:domain` uses `kapt(...)` directly. If that is undesirable, an alternative is to apply the `stoic.android.hilt` plugin in `:core:domain` instead of manual Hilt dependencies.
