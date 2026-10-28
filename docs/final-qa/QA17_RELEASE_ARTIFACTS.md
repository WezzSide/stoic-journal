# QA17 — Final Release Build Verification

Built from the **integration candidate** `integration/mentor-into-main` @ `489cd78b84c9f7fc4699bfb98f0b35b968ad2bee` with `./gradlew :app:assembleRelease :app:bundleRelease` (log `evidence/logs/14_release_integration.log`, BUILD SUCCESSFUL). Signing credentials were supplied **through environment variables only** (the committed `gradle.properties` credentials from `origin/main` were removed first — D-37). Keystore: the developer's local `stoic-journal-release.keystore` (alias `stoicjournal`, CN=StoicJournal, O=Stoic, C=NL) — this keystore currently opens with the default password (D-08) and must be rotated before publication.

First attempt failed with `keystore password was incorrect`: the merge had brought in main's committed `RELEASE_KEY_ALIAS=stoic-alias` etc., which Gradle properties take over the environment — this is how D-37 was discovered.

## Release artefact manifest

| Field | APK | AAB |
|---|---|---|
| Artifact name | `app-release.apk` | `app-release.aab` |
| Artifact type | APK (R8 minified + resource shrinking) | Android App Bundle |
| File size | 3,877,349 bytes | 7,383,513 bytes |
| SHA-256 | `1bd263944c72ce83a2780a3c5027935d97061eb585c77d15eccb2985afa154e9` | `3199cb0bef2a05dbcb8cf9765ab205b9ee1ce4ae46dacc1473fe84fca486fb75` |
| Build variant | release | release |
| Version code / name | 1 / 1.0 | 1 / 1.0 |
| Git commit SHA | 489cd78b84c9f7fc4699bfb98f0b35b968ad2bee | same |
| Signature verification | `apksigner verify`: **Verifies** — APK Signature Scheme v2 ✔ (v1/v3/v3.1/v4 not used); 1 signer, cert SHA-256 `7cf08be2…e08d6cb1` | bundle is JAR-signed (`META-INF/STOICJOU.SF`, `STOICJOU.RSA`); Play re-signs bundles on upload |

Full tool output: `evidence/release_manifest_integration.md`.

## Checks (mission §19)

| Check | Result |
|---|---|
| Package identifier | `com.stoicjournal` ✔ |
| Version code / name | 1 / 1.0 (hard-coded in the application convention plugin — bump before a real release) |
| Supported SDK range | minSdk 26, targetSdk 34, compileSdk 34 ✔ |
| Required permissions (merged) | `INTERNET`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` (app) + library-merged `USE_BIOMETRIC`/`USE_FINGERPRINT` (androidx.biometric), `WAKE_LOCK`/`FOREGROUND_SERVICE`/`ACCESS_NETWORK_STATE` (WorkManager), `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (framework). No unexpected permission. |
| Release signing state | signed with the environment-supplied keystore; **no** default-password fallback remains in the build script |
| APK signature validity | ✔ (`apksigner verify --verbose`) |
| App Bundle generation | ✔ |
| Manifest configuration | launchable activity `MainActivity`; `allowBackup=false`; only `MainActivity` and `BootCompletedReceiver` exported (QA12 §4) |
| Debuggable flag | not present in release badging (`application-debuggable` absent) ✔ |
| Test-only credentials | none (grep for key-like strings in evidence and APK: none) |
| Accidental logging configuration | no logging interceptor; no `Log` strip rule (P3) |
| Sensitive files bundled | none (`keystore`/`jks`/`local.properties`/`pem` absent from the APK) |
| Test-only classes in release | `HiltTestActivity` absent; the string `fake_provider` is present because `FakeAiProvider` is part of the production registry (D-25, hidden from UI and never selected implicitly) |

## Not done (by mission rule)
No upload to Google Play, no publication, signing credentials not exposed. A release built from the *QA branch alone* (`qa/final-qa-001`, i.e. the PR #1 lineage without main's Phase 9/10 additions) was not produced; the integration candidate is the recommended release line.
