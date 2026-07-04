# Task 7 Report: Set up app module, theme, and navigation

## What was implemented

Created the `:app` module and all supporting feature-module placeholders required for the app to compile:

- `app/build.gradle.kts` — application module configuration, depends on all core and feature modules plus Compose, Navigation, Hilt, WorkManager, and Hilt-Work dependencies.
- `app/src/main/java/com/stoicjournal/StoicJournalApplication.kt` — `HiltAndroidApp` Application class implementing `Configuration.Provider` with a `HiltWorkerFactory` for WorkManager.
- `app/src/main/java/com/stoicjournal/MainActivity.kt` — edge-to-edge Compose entry point that observes `MainViewModel.startDestination` and hosts `StoicJournalNavHost`.
- `app/src/main/java/com/stoicjournal/MainViewModel.kt` — seeds assets, schedules reminders if enabled, and chooses `Screen.Today` or `Screen.Onboarding` as the start destination.
- `app/src/main/java/com/stoicjournal/navigation/Screen.kt` — sealed class of all navigation routes with argument helpers.
- `app/src/main/java/com/stoicjournal/navigation/StoicJournalNavHost.kt` — Compose Navigation host wiring all feature screens together.
- `app/src/main/java/com/stoicjournal/theme/{Color,Type,Theme,Shape}.kt` — custom Material3 theme with stone/amber color palette, serif/sans-serif typography, and rounded shapes.
- `app/src/main/res/values/themes.xml` and `strings.xml` — app theme and app_name string.
- `app/src/main/AndroidManifest.xml` — launcher activity and `POST_NOTIFICATIONS` permission.
- `app/src/main/res/mipmap-*/ic_launcher.png` and `ic_launcher_round.png` — placeholder launcher icons for all densities.
- Feature module placeholders in `:feature:today`, `:feature:journal`, `:feature:library`, `:feature:history`, `:feature:settings`, `:feature:onboarding` with minimal `build.gradle.kts` and empty Composables matching the signatures expected by the nav host.

## Supporting fixes required to build

Task 7 was the first code path to exercise the feature convention plugin and full Compose build, so a few pre-existing issues had to be corrected:

- `build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt` — fixed project dependency declarations, added the Compose BOM platform dependency, and enabled Compose / applied the Compose Compiler plugin for feature modules.
- `build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt` — removed unconditional Compose enablement and Compose Compiler plugin application so non-Compose core modules (`:core:domain`, `:core:common`, etc.) compile under Kotlin 2.0.
- `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt` — removed the obsolete `composeOptions.kotlinCompilerExtensionVersion` override so Kotlin 2.0’s Compose Compiler plugin controls the version.
- `core/notifications/src/main/res/drawable/ic_notification.xml` — changed `?attr/colorControlNormal` to `?android:attr/colorControlNormal` so AAPT2 can resolve the attribute without AppCompat.

## Testing and results

Ran the debug assembly:

```bash
./gradlew :app:assembleDebug -Pandroid.aapt2FromMavenOverride=/opt/aapt2-wrapper/aapt2
```

Result: **BUILD SUCCESSFUL** in 38s; `app/build/outputs/apk/debug/app-debug.apk` was produced.

Note: the `-Pandroid.aapt2FromMavenOverride` flag was only necessary because the build environment is an ARM64 (aarch64) Linux container and the AAPT2 binary distributed with AGP 8.5.0 is x86-64. A wrapper script using `box64` was installed at `/opt/aapt2-wrapper/aapt2` to run the x86-64 AAPT2. On a standard x86-64 development machine the project builds with the plain `./gradlew :app:assembleDebug` command.

## Files changed

New files:
- `app/build.gradle.kts`
- `app/src/main/java/com/stoicjournal/StoicJournalApplication.kt`
- `app/src/main/java/com/stoicjournal/MainActivity.kt`
- `app/src/main/java/com/stoicjournal/MainViewModel.kt`
- `app/src/main/java/com/stoicjournal/navigation/Screen.kt`
- `app/src/main/java/com/stoicjournal/navigation/StoicJournalNavHost.kt`
- `app/src/main/java/com/stoicjournal/theme/{Color,Type,Theme,Shape}.kt`
- `app/src/main/res/values/{themes,strings}.xml`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/mipmap-*/ic_launcher.png` and `ic_launcher_round.png`
- All `:feature:*` `build.gradle.kts` and placeholder screen sources.

Modified files:
- `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt`
- `build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt`
- `build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt`
- `core/notifications/src/main/res/drawable/ic_notification.xml`

## Self-review findings

- The navigation graph matches the route/argument structure defined in `Screen.kt`.
- `MainViewModel` correctly awaits preferences and seeds data before emitting the start destination.
- `StoicJournalApplication` configures Hilt’s `WorkManager` factory as required by `DailyReminderWorker`.
- Theme file imports `androidx.compose.ui.graphics.Color` and applies `StoicShapes`.
- Feature placeholders expose the exact callback signatures consumed by `StoicJournalNavHost`.
- The only deviation from the task brief is using `override val workManagerConfiguration` instead of `override fun getWorkManagerConfiguration()`; Kotlin maps this to the same Java getter and the compiler accepted it.

## Issues or concerns

- The launcher icons are simple colored placeholders generated with ImageMagick; they should be replaced with proper adaptive icons before release.
- The `android.aapt2FromMavenOverride` workaround is environment-specific and not committed. The project is expected to build without it on x86-64 hosts.
