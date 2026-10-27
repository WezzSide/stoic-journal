# Stoic Journal

A calm, offline-first Android journaling app for daily Stoic reflection, security, and personal growth.

Stoic Journal helps you build a reflective practice through guided prompts, curated wisdom from Marcus Aurelius, Epictetus, and Seneca, and structured exercises rooted in classical Stoicism. Everything stays on your device — your journal is strictly private by default.

[![Build and Verify Debug APK](https://github.com/WezzSide/stoic-journal/actions/workflows/build.yml/badge.svg)](https://github.com/WezzSide/stoic-journal/actions/workflows/build.yml)

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
core:data/            # Room entities, DAOs, repositories, JSON seeders
core:domain/          # Use cases, models (Virtue, JournalEntry, Streak)
core:common/          # Utilities, date helpers, constants
core:notifications/   # WorkManager workers, DailyReminderWorker, DirectReplyReceiver
```

---

## Getting Started

### Requirements

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK API 34

### Clone

```bash
git clone https://github.com/WezzSide/stoic-journal.git
cd stoic-journal
```

### IDE Setup

Open the project in Android Studio and sync with Gradle.

---

## Building

### Debug APK

```bash
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Signed Release APK & App Bundle

A release upload keystore (`stoic-journal-release.keystore`) is configured in `:app/build.gradle.kts`.

To build the signed release APK and App Bundle (`.aab`):

```bash
./gradlew :app:bundleRelease :app:assembleRelease
```

Output artifacts:
- Release App Bundle: `app/build/outputs/bundle/release/app-release.aab`
- Release Signed APK: `app/build/outputs/apk/release/app-release.apk`

---

## Testing

### Unit tests

```bash
./gradlew test
```

### Instrumentation tests

```bash
./gradlew :app:connectedDebugAndroidTest
```

---

## Privacy & Security

- All journal entries, biometric preferences, and PIN hashes stay 100% local on your device.
- Zero analytics, ads, or online tracking SDKs.
- Privacy Screen (`FLAG_SECURE`) blocks screenshots and obscures app content in the recent apps task manager.
- Microphones for Voice Journaling use native Android `SpeechRecognizer` and process speech locally without transmitting audio.

---

## License

This project is released under the MIT License. See [LICENSE](LICENSE) for details.
