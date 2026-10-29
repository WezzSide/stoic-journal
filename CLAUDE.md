# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## First setup for agents

This checkout contains the complete project, including the root Gradle files, modules, and wrapper.
Do not run `git restore`, `git checkout -- .`, or switch to a sibling checkout to "repair" it.

1. Start at the repository root and inspect `git status --short --branch` and `git log -5 --oneline`. Keep the current branch and preserve all pre-existing changes and untracked files.
2. Use JDK 17 and Android SDK Platform 34 / Build Tools 34.0.0. Open the root folder in Android Studio and let it create the ignored `local.properties` file for the local SDK path.
3. Use the Gradle wrapper, not a separately installed Gradle. On Windows run `.\gradlew.bat`; on macOS/Linux run `./gradlew`.
4. For a first build, run `:app:assembleDebug`. For JVM tests run `test`; connected Android instrumentation tests use `:app:connectedQaAndroidTest` and need an emulator/device.
5. Before changing build versions or shared dependencies, check `gradle/libs.versions.toml` and `build-logic/convention`. Never commit API keys, signing keys, or machine-specific configuration.

The GitHub Actions workflow builds and verifies a debug APK and stores it as a short-lived workflow artifact. It is not a Play Store or GitHub Release deployment.

## Commands

All commands assume the root Gradle files are present (see caveat above).

```bash
./gradlew :app:assembleDebug              # debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease            # release APK (needs signing config, see below)
./gradlew :app:bundleRelease              # release AAB for Play Console

./gradlew test                            # all unit tests (JVM, per-module src/test)
./gradlew :core:domain:test               # unit tests for a single module
./gradlew :feature:journal:test           # e.g. JournalDraftSaverTest

./gradlew :app:assembleDebugAndroidTest   # build instrumentation test APK
./gradlew :app:connectedQaAndroidTest    # run isolated QA instrumentation tests on a connected device/emulator

./gradlew lintDebug                       # Android lint
```

Requirements: JDK 17, Android SDK with platform `android-34` and `build-tools;34.0.0`. Android Studio
can create the ignored `local.properties`; if setting it manually, it must point `sdk.dir` at the SDK.

### Release signing

`app/build.gradle.kts` reads a `release` signing config from `stoic-journal-release.keystore`
(gitignored, at repo root) plus `RELEASE_STORE_PASSWORD` / `RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD`
from `~/.gradle/gradle.properties` or environment variables. Without these, `assembleRelease` /
`bundleRelease` will fail or produce an unsigned artifact.

### AAPT2 on aarch64 (Termux/PRoot only)

Only relevant on ARM Linux hosts (e.g. Termux PRoot), **not** standard x86_64 workstations. AGP's
Maven-downloaded AAPT2 is x86_64-only there; force AGP to use the SDK's `qemu-x86_64`-wrapped AAPT2:

```properties
# ~/.gradle/gradle.properties
android.aapt2FromMavenOverride=/path/to/android-sdk/build-tools/34.0.0/aapt2
```

Full root-cause writeup: `docs/build-environment.md`.

### On-device smoke test (Shizuku)

`./scripts/shizuku_verify.sh app/build/outputs/apk/debug/app-debug.apk` installs the APK via `rish`,
launches `MainActivity`, and fails on fatal logcat exceptions. Requires a running Shizuku service.

## Architecture

Multi-module Gradle project (root name `StoicJournal`, package `com.stoicjournal`), Kotlin + Jetpack
Compose, MVVM + Repository, with local-first Room storage and optional network-backed AI providers.

### Module graph

```
app/                  application shell: MainActivity, nav host, theme, DI entry point
feature:today/         home screen — daily prompt, streak, quick actions
feature:journal/       guided / free-form / exercise journal entry flows
feature:library/       quote + philosopher browsing, favorites
feature:history/       entry list, filters, detail, swipe-to-delete
feature:settings/      reminder time, reset onboarding, attribution
feature:onboarding/    first-launch flow (Stoicism-familiarity level, reminder opt-in)
feature:ai/            AI reflection and Stoic Mentor UI
core:data/             Room entities/DAOs/database, repository impls, asset-JSON seeder
core:domain/           models, repository interfaces, use cases (no Android deps)
core:common/           app-level CoroutineScope qualifier, shared DI utilities
core:notifications/    WorkManager daily reminder worker + scheduler
```

`app` depends on every `feature:*` and `core:*` module. `feature:*` modules depend on `core:domain` and
`core:common` only — never on `core:data` or each other directly; persistence and provider access go
through domain repository interfaces injected via Hilt.

### Convention plugins (`build-logic/convention`)

Module-level `build.gradle.kts` files are thin — they apply one of the composite build's own plugin IDs
and almost nothing else. Shared config (compileSdk 34, minSdk 26, targetSdk 34, JVM 17, Compose BOM,
Hilt/kapt wiring) lives in the plugin classes, not the module scripts:

| Plugin ID | Class | Applies to |
|---|---|---|
| `stoic.android.application` | `AndroidApplicationConventionPlugin` | `app` — also applies `stoic.android.hilt` |
| `stoic.android.library` | `AndroidLibraryConventionPlugin` | base for library/feature modules |
| `stoic.android.feature` | `AndroidFeatureConventionPlugin` | `feature:*` — layers Compose + Hilt + adds `core:domain`/`core:common` deps automatically |
| `stoic.android.hilt` | `AndroidHiltConventionPlugin` | adds Hilt runtime + kapt compiler |
| `stoic.android.room` | `AndroidRoomConventionPlugin` | `core:data` — adds Room runtime/ktx/compiler |

When changing SDK/Kotlin/Compose versions or adding a dependency shared across feature modules, edit the
relevant convention plugin (or `gradle/libs.versions.toml`) rather than individual module build files.

### Data & seeding

`core:data`'s `AssetSeedImporter` (`core/data/.../seed/AssetSeedImporter.kt`) does a one-time import of
bundled JSON (`core/data/src/main/assets/{quotes,prompts,exercises,philosophers}.json`) into Room inside
a single transaction, gated on all four tables being empty — this is the only path that populates
reference content; journal entries themselves are always user-created. `DataModule.kt` wires the Room
`Singleton` database and binds every `*RepositoryImpl` to its `core:domain` interface via `@Binds`.
Journal data has no cloud sync. Optional AI requests send user prompts and included conversation
context to the provider selected in Settings; do not describe the whole app as offline-only.

### Navigation

Single `NavHost` in `app/.../navigation/StoicJournalNavHost.kt`, routes defined in `Screen.kt`. Screens
that take an ID argument (`GuidedJournal`, `ExerciseJournal`, `QuoteDetail`, `EntryDetail`) guard against
a missing/null nav argument by logging and popping the back stack rather than crashing.

### Journal draft autosave

`feature:journal`'s `JournalDraftSaver` is a small reusable debounced-autosave helper (shared by the
guided/free-form/exercise ViewModels): callers give it a `JournalDraft` producer and a suspend save
callback, and it debounces writes and flushes on exit. `JournalDraftRepository` persists these through
`core:data`.

### Testing layout

- JVM unit tests are in module-level `src/test` folders, including `core/domain`, `feature/journal`,
  and `feature/ai`.
- Instrumentation tests are in `app/src/androidTest` and feature modules' `src/androidTest`.
  `app` sets `testBuildType = "qa"` and uses the isolated `com.stoicjournal.qa` application ID;
  run `:app:connectedQaAndroidTest` on a connected device/emulator.

## Other repo-root docs

`docs/` holds product planning artifacts (`prd.md`, `product-roadmap.md`, `product-vision.md`,
`build-environment.md`) and `.autoloop/` holds output from a prior autonomous-agent run (gap analysis,
decisions, gate results) — these are historical/reference, not living specs to keep in sync on every
change.
