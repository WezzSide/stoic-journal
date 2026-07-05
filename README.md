# Stoic Journal

A calm, offline-first Android journaling app for daily Stoic reflection.

Stoic Journal helps you build a reflective practice through guided prompts,
curated wisdom from Marcus Aurelius, Epictetus, Seneca, and structured
exercises rooted in classical Stoicism. Everything stays on your device — your
journal is private by default.

[![Build and Verify Debug APK](https://github.com/B0LK13/stoic-journal/actions/workflows/build.yml/badge.svg)](https://github.com/B0LK13/stoic-journal/actions/workflows/build.yml)

---

## Table of Contents

- [Why Stoic Journal](#why-stoic-journal)
- [Features](#features)
- [Screenshots](#screenshots)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Building](#building)
- [Distribution](#distribution)
- [Testing](#testing)
- [On-Device Verification with Shizuku](#on-device-verification-with-shizuku)
- [Privacy](#privacy)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## Why Stoic Journal

Philosophy is a practice, not a lecture. Reading a Stoic quote is useful, but
real change comes from reflecting on it and applying it to your own life. Stoic
Journal makes that practice:

- **Private** — journal entries never leave the device in the MVP.
- **Focused** — no feeds, social features, or distractions.
- **Grounded** — primary-source quotes with attribution, not generic
  motivational text.
- **Practical** — guided exercises like the Dichotomy of Control, Evening
  Review, Premeditatio Malorum, View from Above, and Amor Fati.

---

## Features

### MVP v1.0

- **Onboarding** — choose your familiarity with Stoicism, set a daily reminder,
  and seed bundled content on first launch.
- **Today / Home** — a fresh daily Stoic prompt with a related quote, quick
  actions for guided reflection, free-form entry, or a Stoic exercise, plus a
  consistency indicator.
- **Journal Entry**
  - *Guided* — pre-filled prompt and quote.
  - *Free-form* — blank title and body, optional quote attachment.
  - *Exercise* — step-by-step guidance with a final reflection prompt.
- **Wisdom Library** — browse quotes by philosopher or theme, favorite quotes,
  and view full attribution.
- **Stoic Exercises** — five classical exercises with explanations and guided
  steps.
- **History** — chronological entries, filter by type, detail view, and
  swipe-to-delete with undo.
- **Daily Reminder** — local notification at your chosen time, powered by
  WorkManager.
- **Settings** — change/disable reminders, reset onboarding, and view
  attribution.

---

## Screenshots

*Screenshots will be added here after the first release build is captured on a
device.*

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI Toolkit | Jetpack Compose |
| Design | Material 3 with a custom calm, earthy theme |
| Architecture | MVVM + Repository pattern |
| Dependency Injection | Hilt |
| Local Database | Room (SQLite) |
| Async | Kotlin Coroutines + Flow |
| Background Work | WorkManager |
| Preferences | DataStore |

### Module Structure

```text
app/                  # Application shell, navigation, theme
feature:today/        # Home screen and daily prompt
feature:journal/      # Guided, free-form, and exercise entry flows
feature:library/      # Quote and philosopher browsing
feature:history/      # Entry list and detail
feature:settings/     # Reminders and preferences
feature:onboarding/   # First-launch onboarding
core:data/            # Room entities, DAOs, repositories, seeders
core:domain/          # Use cases and models
core:common/          # Utilities, date helpers, constants
core:notifications/   # Daily reminder worker and scheduler
```

---

## Getting Started

### Requirements

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK with API 34 and build-tools 34.0.0

### Clone

```bash
git clone https://github.com/B0LK13/stoic-journal.git
cd stoic-journal
```

### IDE Setup

Open the project in Android Studio and sync with Gradle. The project uses
composite build logic under `build-logic/`, so the first sync may take a few
minutes.

---

## Building

### Debug APK

```bash
./gradlew :app:assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Release APK

A release keystore is required. One has been generated for this project:

```text
stoic-journal-release.keystore
```

The keystore is **gitignored**; you must keep your own copy safe. Signing
properties are read from `~/.gradle/gradle.properties` or environment variables:

```properties
RELEASE_STORE_PASSWORD=<password>
RELEASE_KEY_ALIAS=<alias>
RELEASE_KEY_PASSWORD=<password>
```

Build the release APK:

```bash
./gradlew :app:assembleRelease
```

Output:

```text
app/build/outputs/apk/release/app-release.apk
```

### AAPT2 workaround on ARM/Termux

On ARM Linux containers (e.g., PRoot/Ttermux), AGP’s bundled AAPT2 is x86_64
and cannot run. Force AGP to use the SDK AAPT2 wrapper:

```properties
# ~/.gradle/gradle.properties
android.aapt2FromMavenOverride=/path/to/android-sdk/build-tools/34.0.0/aapt2
```

See [docs/build-environment.md](docs/build-environment.md) for the full root
cause and workaround.

---

## Distribution

### GitHub Releases

1. Bump `versionCode` and `versionName` in the app convention plugin
   (`build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt`).
2. Build a signed release APK:
   ```bash
   ./gradlew :app:assembleRelease
   ```
3. Create a new GitHub Release and attach `app-release.apk`.

### Google Play

For Play Store distribution, build an Android App Bundle:

```bash
./gradlew :app:bundleRelease
```

Upload `app/build/outputs/bundle/release/app-release.aab` to Google Play
Console.

> **Note:** The committed release signing config uses a project-level keystore.
> For Play Store, replace it with your Play App Signing key or keep the existing
> upload key and enroll in Play App Signing.

---

## Testing

### Unit tests

```bash
./gradlew test
```

### Instrumentation tests

Build the test APK:

```bash
./gradlew :app:assembleDebugAndroidTest
```

Run on a connected device or emulator:

```bash
./gradlew :app:connectedDebugAndroidTest
```

### On-device smoke test

If you have Shizuku running, use the included script:

```bash
./scripts/shizuku_verify.sh app/build/outputs/apk/debug/app-debug.apk
```

---

## On-Device Verification with Shizuku

The project includes a Shizuku-powered verification script that installs the
APK, checks package metadata, launches the main activity, and scans logcat for
fatal exceptions.

Verified results on the reference device:

- Package: `com.stoicjournal`
- Version: `1.0` (versionCode 1)
- minSdk: 26, targetSdk: 34
- Debug APK install: Success
- Release APK install: Success
- Launch: `com.stoicjournal/.MainActivity`
- Process: Running
- Logcat: No fatal exceptions

See [scripts/shizuku_verify.sh](scripts/shizuku_verify.sh) and
[docs/build-environment.md](docs/build-environment.md).

---

## Privacy

- All journal entries, favorites, and preferences are stored locally.
- No analytics, crash reporting, or cloud sync in the MVP.
- The app requests only the permissions it needs:
  - `POST_NOTIFICATIONS` for the optional daily reminder
  - `RECEIVE_BOOT_COMPLETED` to reschedule reminders after reboot

---

## Roadmap

See [docs/product-roadmap.md](docs/product-roadmap.md) for the full plan.

High-level phases:

1. **MVP v1.0** — daily reflection, library, exercises, history, reminders.
2. **Habit Reinforcement v1.1** — widget, summaries, export, search, streak
   insights.
3. **Depth & Learning v1.2** — themed collections, philosopher deep-dives,
   lesson cards.
4. **Connected Practice v2.0** — optional encrypted sync, audio reflections,
   Wear companion.

---

## Contributing

Contributions are welcome. Please open an issue or pull request with a clear
description of the change. For larger features, discuss the approach in an issue
first.

---

## License

This project is released under the MIT License. See [LICENSE](LICENSE) for
details.

Content (quotes, prompts, exercises) is derived from public-domain Stoic texts.
