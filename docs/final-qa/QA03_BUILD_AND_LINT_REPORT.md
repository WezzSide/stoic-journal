# QA03 — Build & Static Analysis Report

All commands run from the worktree with the WSL SDK (`local.properties` → `/home/Admin/Android/Sdk`, see QA00). Logs are under `evidence/logs/`.

## 1. Baseline (commit 568cbc7, before any fix)

| Command | Result | Notes / log |
|---|---|---|
| `./gradlew :app:assembleDebug` (Windows SDK path) | **FAILED** | `Installed Build Tools revision 35.0.0 is corrupted` — Windows build-tools binaries unusable from Linux; environment issue, not a project defect. `01_assembleDebug_failed.log` |
| `./gradlew :app:assembleDebug` (WSL SDK + android-34) | **SUCCESS** in 10 m 34 s, 311 tasks | 12 compiler warnings, all `Kapt currently doesn't support language version 2.0+. Falling back to 1.9.`; 0 deprecation warnings surfaced by Gradle. APK 22 MB. `02_assembleDebug.log` |
| `./gradlew test --continue` (debug + release unit tests) | **SUCCESS** — 39 tests, 0 failures, 0 skipped | 11 test classes. `03_unitTests_baseline.log` |
| `./gradlew lintDebug --continue` | **FAILED** (tooling) | `:feature:library:lintAnalyzeDebug` crashed: `NullPointerException` in Compose lint `ComposableCoroutineCreationDetector` (`ComposableUtils.kt:162`) — "this is a bug in lint or one of the libraries it depends on". The other 10 library modules produced reports (below). The app module was not analysed because of the dependency failure. `04_lint_baseline.log` |

Baseline lint findings (10 modules that completed): **0 errors, 9 warnings** — `KaptUsageInsteadOfKsp` ×1 (core/data), `ObsoleteSdkInt` ×4 (core/notifications), `ModifierParameter` ×3 (feature/ai) ×1 (feature/journal). No security-category lint findings.

## 2. After remediation (`qa/final-qa-001`)

| Command | Result | Log |
|---|---|---|
| `./gradlew :app:assembleDebug testDebugUnitTest` (1st run) | kapt error: missing import of `AiProviderSelectionRepositoryImpl` in `DataModule` → fixed | `05_build_after_fixes.log` |
| `./gradlew :app:assembleDebug testDebugUnitTest` (2nd run) | compile OK; **4 provider streaming tests FAILED** with `IllegalStateException: Flow invariant is violated` → genuine P0 (D-32) in shipped code, fixed | `06_build_after_fixes.log` |
| `./gradlew :core:data:testDebugUnitTest :core:domain:testDebugUnitTest` | SUCCESS | `07_unit_core.log` |
| `./gradlew :app:assembleDebug testDebugUnitTest :app:assembleDebugAndroidTest :core:data:assembleDebugAndroidTest` | app + unit tests SUCCESS (**88 tests, 0 failures**); `:app:compileDebugAndroidTestKotlin` failed on a wrong import in two new test files → fixed | `08_full_build_tests.log` |
| `./gradlew :app:assembleDebugAndroidTest :core:data:assembleDebugAndroidTest` | SUCCESS | `09_androidtest_apks.log` and later rebuilds `10_…`, `11_…`, `12_…` (test-fixture iterations) |
| `./gradlew lintDebug --continue` (with the scoped workaround) | see §4 | `13_lint_after.log` |
| `./gradlew :app:assembleRelease :app:bundleRelease` | see QA17 | `14_release.log` |

Unit-test totals after remediation (debug variant, JUnit XML): **88 tests / 0 failures / 0 errors / 0 skipped** across 16 classes (baseline 39 / 11). New classes: `MentorConversationCoordinatorTest` (8), `ProactiveReflectionEligibilityEvaluatorTest` (10), `ProactiveReminderTimingTest` (6), `OpenAiProviderTest` (10), `AnthropicClaudeProviderTest` (5); extended: `ContextualMemoryTest` 1→8, `DefaultAiProviderRegistryTest` 3→6.

## 3. Lint crash workaround (D-27)
`feature/library/build.gradle.kts` now disables **one** check, `CoroutineCreationDuringComposition`, for that module only, with a comment pointing here. No baseline file, no global suppression. Root cause is an upstream incompatibility between Compose UI lint checks 1.6.x (BOM 2024.06.00) and AGP 8.13 lint; upgrading the Compose BOM would remove the need but is out of scope for a release-acceptance mission.

## 4. Lint after remediation

`./gradlew lintDebug --continue` → **BUILD SUCCESSFUL**, all 12 modules analysed (`13_lint_after.log`). **0 errors, 69 warnings**: `app` 60 (`GradleDependency` ×24, `NewerVersionAvailable` ×12, `IconLauncherShape` ×10, `IconDuplicates` ×5, `AndroidGradlePluginVersion` ×4, `UnusedResources` ×2, `BomWithoutPlatform` ×1, `DataExtractionRules` ×1), `core/notifications` 4 (`ObsoleteSdkInt`), `feature/ai` 3 + `feature/journal` 1 (`ModifierParameter`), `core/data` 1 (`KaptUsageInsteadOfKsp`). No security/correctness-category findings. Warnings were not suppressed.

## 5. Dependency audit (mission §5.2)

| Item | Finding | Risk | Action |
|---|---|---|---|
| `com.google.ai.client.generativeai:generativeai:0.9.0` (Gemini) | Google AI client SDK for Android is **deprecated**; repo renamed `deprecated-generative-ai-android`, "no further updates"; replacement is Firebase AI Logic (`firebase-ai`) which requires a Firebase project ([migration guide](https://firebase.google.com/docs/ai-logic/migrate-from-google-ai-client-sdks)) | Medium: no security fixes; still functional against current models | Documented; migration is a product decision (Firebase dependency) — not performed. Removed the unused copy of the dependency from `feature:ai`. |
| `androidx.biometric:biometric:1.2.0-alpha05` | alpha artifact in a release build | Low | Documented |
| `com.squareup.okhttp3:okhttp:4.12.0` | no open advisory found for 4.12.0; historical CVEs (2021-0341, 2022-24329, 2023-0833) are fixed in ≤4.10/4.9.2 ([Snyk](https://security.snyk.io/package/maven/com.squareup.okhttp3:okhttp/4.9.0)) | Low | Keep |
| `com.squareup.okhttp3:logging-interceptor` | unused | Hygiene | Removed |
| `com.google.android.libraries.mapsplatform.secrets-gradle-plugin:2.0.1` | injected `local.properties` into `BuildConfig` (D-16) | Medium (secret leakage into debug builds) | Removed |
| AGP 8.13.2 / Gradle 8.13 / Kotlin 2.0.21 / kapt | kapt forced to language level 1.9 (warning); KSP migration recommended by lint | Low | Documented |
| Compose BOM 2024.06.00, Navigation 2.7.7, Lifecycle 2.8.2, Room 2.6.1, Hilt 2.51.1, WorkManager 2.9.0, DataStore 1.1.1, kotlinx-serialization 1.6.3 | 2024-era but supported; no known CVEs identified for these versions in the searches performed | Low | Keep |
| androidx.test (Espresso 3.6.1, runner 1.6.2, ext-junit 1.2.1) | **broken on Android 17 / API 37** (`NoSuchMethodException: InputManager.getInstance`) → every instrumentation test failed to inject input | Test-only | Upgraded to Espresso 3.7.0, runner/rules 1.7.0, ext-junit 1.3.0 (+ uiautomator 2.3.0, work-testing for the suite) |
| Manifest permissions | `INTERNET`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` used; `ACCESS_NETWORK_STATE` had **no user** (no `ConnectivityManager` reference) | Hygiene | Removed from the manifest |
| Debug-only components in release | `HiltTestActivity` lives in `src/debug` only; `FakeAiProvider` in main (see D-25) | Low | Documented |

## 6. Reproducibility
Clean worktree checkout + documented toolchain builds without hidden local files (the only local input is the gitignored `local.properties`). Release signing requires the three `RELEASE_*` inputs and the gitignored keystore; otherwise the release artifact is unsigned by design (D-08). No secrets, SDK paths or credentials were committed (`git ls-files` check in QA12 §7).
