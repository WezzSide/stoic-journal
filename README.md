# Stoic Journal

A calm Android journaling app for daily Stoic reflection, security, and personal growth.

Stoic Journal helps you build a reflective practice through guided prompts, curated wisdom from Marcus Aurelius, Epictetus, and Seneca, structured exercises, and an optional AI Stoic Mentor. Journal entries are stored locally. AI features send the text and context needed for a request to the provider you configure; review that provider's privacy terms before using them.

[![Build and Verify Debug APK](https://github.com/WezzSide/stoic-journal/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/WezzSide/stoic-journal/actions/workflows/build.yml)

---

## Table of Contents

- [Why Stoic Journal](#why-stoic-journal)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Building](#building)
- [Distribution](#distribution)
- [Testing](#testing)
- [Privacy & Security](#privacy--security)
- [License](#license)

---

## Why Stoic Journal

Philosophy is a practice, not a lecture. Reading a Stoic quote is useful, but real change comes from reflecting on it and applying it to your own life. Stoic Journal makes that practice:

- **Private & Secure** — Biometric & PIN lock protection with `FLAG_SECURE` privacy screen; journal entries never leave your device.
- **Focused** — No social feeds, ads, or online tracking.
- **Grounded** — Primary-source quotes with full attribution from Marcus Aurelius, Seneca, and Epictetus.
- **Practical** — Guided exercises like the Dichotomy of Control, Seneca's Evening Review, Premeditatio Malorum, View from Above, and Amor Fati.

---

## Features

### Core Capabilities

- **App Security & Privacy:**
  - Biometric authentication (fingerprint / face unlock via `androidx.biometric`).
  - 4-digit PIN fallback hashed with SHA-256 in DataStore.
  - Configurable lock timeout (Immediate, 1 min, 5 min).
  - `FLAG_SECURE` Privacy Screen to block screenshots and obscure app content in recent apps.

- **Today / Home Screen:**
  - Daily Stoic prompt and matching classical quote.
  - Quick action buttons for Free-Form Journaling and Guided Exercises.
  - **On This Day (Terugblik):** Revisit thoughts and entries written on this day in past weeks or months.
  - **Compassionate Streak Counter:** Streak protection with a 1-day freeze logic.

- **Journal Editor & Prompts:**
  - **Guided & Free-form:** Blank title, quote attachments, and 1-click **"Geef mij een vraag 💡"** prompt selection bar.
  - **Voice Journaling:** Integrated Android Speech-to-Text (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) microphone button to dictate entries.
  - **Virtue Tagging:** Tag entries with the 4 Cardinal Virtues (🧠 Wijsheid, 🦁 Moed, ⚖️ Rechtvaardigheid, 🍇 Matigheid).

- **AI Stoic Mentor:** Chat with a configurable AI provider, review past conversations, and turn a conversation into a draft reflection. AI use requires your own provider credentials and an internet connection.

- **Stoic Exercises:**
  - **Dichotomy of Control Tool:** Interactive 2-column wizard (*Binnen mijn controle* vs. *Buiten mijn controle*) that automatically formats reflections into structured entries.
  - Amor Fati, Evening Review, Premeditatio Malorum, and View from Above step-by-step exercises.

- **Wisdom Library & History:**
  - Browse quotes by philosopher or theme, favorite quotes, and view full attributions.
  - Chronological history with category filters, swipe-to-delete, and **De 4 Kardinaaldeugden Statistiek** progress card.

- **Dual WorkManager Reminders:**
  - **Morning Intentions (08:00):** *Premeditatio Malorum* notification.
  - **Evening Review (21:00):** *Seneca's Avondreview* notification.
  - **Direct Reply & Quick Check-in:** Type and save journal entries directly from notifications using `RemoteInput` without opening the main UI.

- **Local Backup & Export:**
  - Local JSON export and import options in Settings using Android Storage Access Framework (`CreateDocument` / `OpenDocument`).

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI Toolkit | Jetpack Compose (Material 3) |
| Architecture | MVVM + Repository Pattern + Clean Architecture |
| Dependency Injection | Hilt |
| Local Database | Room (SQLite) |
| Preferences | Jetpack DataStore |
| Async | Kotlin Coroutines + Flow |
| Background Work | WorkManager |
| Security | AndroidX Biometric, SHA-256 Hashing, Window `FLAG_SECURE` |

### Module Structure

```text
app/                  # Shell, Navigation, MainViewModel, Security LockScreen & BiometricPromptManager
feature:today/        # Home screen, daily prompt, streak, OnThisDayCard
feature:journal/      # Free-form, guided, exercise entry, PromptSelectionBar, VirtueTagPicker
feature:library/      # Quote and philosopher browsing
feature:history/      # Entry list, detail, and VirtueStatsCard
feature:settings/     # Dual reminders, PIN/Biometric lock, JSON backup/restore
feature:onboarding/   # First-launch onboarding
feature:ai/          # AI reflection and Stoic Mentor UI
core:data/            # Room entities, DAOs, repositories, JSON seeders
core:domain/          # Use cases, models (Virtue, JournalEntry, Streak)
core:common/          # Utilities, date helpers, constants
core:notifications/   # WorkManager workers, DailyReminderWorker, DirectReplyReceiver
```

---

## Getting Started

### Requirements

- JDK 17
- Android Studio with Android SDK Platform 34 and Build Tools 34.0.0

### Clone

```bash
git clone https://github.com/WezzSide/stoic-journal.git
cd stoic-journal
```

### First run

1. Open the repository root in Android Studio and let it create `local.properties` for your Android SDK. This file is local and must not be committed.
2. Select a JDK 17 Gradle JDK and sync the project.
3. Build and install the debug app:

   ```powershell
   .\gradlew.bat :app:installDebug
   ```

   On macOS/Linux, use `./gradlew :app:installDebug`.

The AI Mentor is optional. To use it, add an API key for a supported provider in the app's AI settings. Never commit API keys, signing keys, or machine-specific configuration.

---

## Building

### Debug APK

Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

macOS/Linux:

```bash
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK & App Bundle

Release builds are unsigned unless a release keystore (`stoic-journal-release.keystore`) and all three credentials are supplied: `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`. Store them in your user-level Gradle properties or environment; never put them in the repository.

Build release artifacts:

```bash
./gradlew :app:bundleRelease :app:assembleRelease
```

Output artifacts:
- Release App Bundle: `app/build/outputs/bundle/release/app-release.aab`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

The GitHub Actions workflow builds and uploads a debug APK artifact; it does not publish to Google Play or create a GitHub Release. Release distribution requires a separate release process.

---

## Testing

### Unit tests

```bash
./gradlew test
```

### Instrumentation tests

```bash
./gradlew :app:connectedQaAndroidTest
```

The QA application uses a separate `com.stoicjournal.qa` application ID to avoid modifying data in an installed production/debug app.

---

## Privacy & Security

- Journal entries and app data are stored locally on your device; the app does not provide cloud journal sync.
- AI prompts, conversation context, and related text are sent to the AI provider selected in Settings when you use AI features. Provider credentials are configured in the app; do not share them or commit them.
- Privacy Screen (`FLAG_SECURE`) blocks screenshots and obscures app content in the recent apps task manager.
- Voice journaling uses Android speech recognition. Whether audio is processed on-device depends on the device's speech-recognition service and settings.

---

## License

This project is released under the MIT License. See [LICENSE](LICENSE) for details.
