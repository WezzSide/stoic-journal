# QA01 — Test Environment

| Item | Value | Source |
|---|---|---|
| Repository | `WezzSide/stoic-journal` (remote URL `https://github.com/B0LK13/stoic-journal.git` redirects there) | `git remote -v`, `gh repo view` |
| Working branch (mission) | `feature/stoic-mentor-multiprovider` | `git branch` |
| Verified initial SHA (mission) | `712fe71730ac68906b39fcb72a3e63d6d1a013c1` — confirmed ancestor of HEAD | `git merge-base --is-ancestor` |
| HEAD at mission start | `568cbc7071e85b587a5ce084404e029778797fda` (`docs: finalize AI-7 release reports…`, one docs commit after 712fe71) | `git rev-parse HEAD` |
| QA work branch | `qa/final-qa-001` in worktree `.claude/worktrees/final-qa` (created from 568cbc7) | `git worktree list` |
| Android Studio | `AI-261.23567.138.2611.15646644` (Windows install, not driven from the shell) | `product-info.json` |
| Gradle | 8.13 (wrapper) | `gradle-wrapper.properties` |
| AGP | 8.13.2 | `libs.versions.toml` |
| Kotlin | 2.0.21 (kapt falls back to 1.9 language level — build warning) | `libs.versions.toml`, build log |
| Compose BOM / Hilt / Room / WorkManager | 2024.06.00 / 2.51.1 / 2.6.1 / 2.9.0 | `libs.versions.toml` |
| Build JDK | Daemon toolchain JDK 17 (Microsoft, provisioned by foojay); host JDK Temurin 21.0.12.1 | `gradle/gradle-daemon-jvm.properties`, `java -version` |
| Android SDK used for builds | `/home/Admin/Android/Sdk` (WSL): build-tools 35.0.0 & 37.0.0, platforms android-34 (installed during QA00), android-35, android-37.0, cmdline-tools latest, platform-tools | `ls`, `sdkmanager` |
| compileSdk / minSdk / targetSdk | 34 / 26 / 34 | `build-logic/convention/.../AndroidApplicationConventionPlugin.kt` |
| Physical device | Google Pixel 10 Pro (`blazer`), Android 17, API 37, build `CP3A.261005.005`, user build, 1080×2410, 420 dpi (override 356) | `adb.exe shell getprop`, `wm size/density` |
| Device serial | `adb-55211FDCH008GQ-N60HOw._adb-tls-connect._tcp` (wireless debugging, reachable only via the Windows `adb.exe`) | `adb.exe devices -l` |
| App state on device at start | `com.stoicjournal` **not installed** → no personal data at risk; all installs are fresh test installs | `pm list packages` |
| Emulators | None available in WSL; Windows AVD `medium_phone` (API 36 image) and API 37 image present, not used (no API ≤34 image locally; downloads avoided) | QA00 |
| Build variants under test | `debug` (device + instrumentation), `release` (artifact verification only) | — |
| Test start timestamp | 2026-10-28 ~15:05 CET (first `assembleDebug` launched) | job logs |

## Test data isolation

* Only synthetic identifiers are used (`QA Journal Entry 001`, `QA Conversation 001`, `QA Memory 001`, `QA Reflection 001`, `QA Proactive …`).
* Database/migration tests use a dedicated file `qa_migration_test.db` or Room in-memory databases, never `stoic_journal.db`.
* `pm clear` / uninstall were not needed and were not run against any pre-existing install (there was none).
* Live AI providers were never called; the Mentor journeys use the explicitly selected offline `FakeAiProvider`; HTTP providers are tested against `MockWebServer`.

## Reproducibility notes

* A clean checkout builds with the documented toolchain once `local.properties` points at a Linux SDK that has `platforms;android-34` — this is exactly what CI (`.github/workflows/build.yml`) installs. The committed `local.properties` is gitignored; no machine-specific path is committed.
* Release signing requires `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` (Gradle properties or environment). After this mission's fix the build no longer falls back to default passwords; without them the release artifact is produced unsigned and the build prints a warning (see QA17).
