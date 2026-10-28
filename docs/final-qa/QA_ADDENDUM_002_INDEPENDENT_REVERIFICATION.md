# QA Addendum 002: independent re-verification and follow-up remediation

Branch: `qa/final-qa-002`, created from `qa/final-qa-001` @ `4cf2807`. No history rewrite. Nothing merged.
Host: Windows (Android Studio 2026.2.1, its Gradle/IDE integration, JDK 17.0.20.1 Microsoft, adb 1.0.41 / platform-tools 37.0.1). QA-001 ran under WSL.

## 1. Purpose
QA-001 produced an evidence package with verdict C (`SJ_FINAL_INTEGRATION_BLOCKED`). This pass:
1. Re-checks QA-001's verifiable claims on a different host. It does not repeat them on trust.
2. Fixes open items that can be fixed safely.
3. Records what is still unverified.

## 2. Findings about the repository's prior state
| Observation | Evidence | Consequence |
|---|---|---|
| `feature/stoic-mentor-multiprovider` HEAD was `568cbc7`, one docs commit after the mission SHA `712fe71`. `712fe71` is an ancestor. | `git merge-base --is-ancestor` → 0 | Consistent with QA-001 |
| The main checkout contained **12 untracked `docs/final-qa/QA00–QA11` files**, all "PASS", from an earlier session. They contradicted the committed QA-001 evidence. | content (for example, QA06 claims streamed TextDeltas and preserved history, which D-01, D-32 and D-34 make impossible at that SHA) | Moved out of the working tree, not deleted, to the agent scratch dir `scratch/stale-untracked-final-qa/`. Already rejected in SJ_FINAL_ACCEPTANCE_REPORT §4. |
| PR #2 (integration candidate → `main`) and PR #3 (QA → feature) are draft/MERGEABLE. PR #1 is CONFLICTING. | `gh pr list` | Matches QA16 |
| `origin/main` and `qa/final-qa-001` are both ancestors of `integration/mentor-into-main`. | `git merge-base --is-ancestor` → 0/0 | Integration candidate contains both sides |

## 3. Independent re-verification (Windows host)
| Check | QA-001 claim | QA-002 result |
|---|---|---|
| JVM unit tests | 88/88 | **Reproduced: 88/88** (core:data 30, core:domain 45, feature:ai 8, feature:journal 5), debug and release variants |
| `assembleDebug` | success | Reproduced |
| Lint, all modules | 0 errors with scoped `feature:library` workaround | Reproduced: **0 errors** in every module. app has 59 warnings (24 GradleDependency, 12 NewerVersionAvailable, 10 IconLauncherShape, 5 IconDuplicates, 5 ObsoleteSdkInt, 3 AGP version, 2 UnusedResources, KaptUsageInsteadOfKsp, BomWithoutPlatform, DataExtractionRules); see D-38 |
| `:app:assembleRelease` / `:app:bundleRelease` | success | Reproduced. Without `RELEASE_*` credentials on this host the APK is `app-release-unsigned.apk`, which is the intended D-08 behaviour (no default-password fallback) |
| Release merged manifest | debuggable off, no test components | Verified. No `android:debuggable`, no `HiltTestActivity`. Exported components are only the launcher activity, the boot receiver (it checks the protected `BOOT_COMPLETED` action), the Glance widget receiver, and AndroidX services/receivers guarded by `BIND_*`/`DUMP` permissions. Permissions: INTERNET, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, plus library-merged USE_BIOMETRIC/USE_FINGERPRINT, WAKE_LOCK, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE (from WorkManager/biometric) |

## 4. Remediation in QA-002
| ID | Change | Proof |
|---|---|---|
| D-24 (P2) | Persisted PIN brute-force throttle (`PinAttemptPolicy` in core:domain; `PreferencesRepository.recordPinFailure/resetPinFailures`; `MainViewModel.unlockWithPin` refuses while throttled; `LockScreen` countdown with keypad disabled) | `PinAttemptPolicyTest` 4/4 pass. New device test `AppLockDeepLinkE2ETest.repeatedWrongPins_…` compiled (**device run pending**) |
| D-38 (P2) | Backup/data-extraction rules wired in the manifest and corrected | lint `DataExtractionRules` gone, 0 errors, merged manifest |
| D-39 (P1, test safety) | `qa` build type `com.stoicjournal.qa` is now the `testBuildType`. Device tests can no longer modify the owner's installed app data (QA-001's lock test cleared the PIN of the installed app) | `:app:assembleQa :app:assembleQaAndroidTest` green |
| Wisdom Library gap (was NOT_TESTED) | `LibraryViewModelTest`: load, theme filter, philosopher filter, favourite toggle in both directions, write-failure error path and recovery | 5/5 pass |

Regression after the changes: **97/97 JVM tests pass** (core:data 30, core:domain 49, feature:ai 8, feature:journal 5, feature:library 5) in debug and release variants. `assembleRelease`, `bundleRelease`, `assembleDebug`, `assembleQa` and `assembleQaAndroidTest` are green. App lint has 0 errors.

## 4b. Device run (Pixel 10 Pro, Android 17 / API 37, wireless debugging, 2026-10-28 18:07)

The owner made the device available. The first attempt was invalid: the phone was attached twice (USB plus wireless), so Gradle ran the suite on both transports at once. The wireless run's reinstall killed the USB run's instrumentation ("Process crashed" / "Unable to find instrumentation target package"). The owner unplugged USB and the suite was re-run on one transport.

| Suite | Package under test | Result |
|---|---|---|
| `:app:connectedQaAndroidTest` | `com.stoicjournal.qa` (isolated; the owner's `com.stoicjournal` was **not** installed over or modified) | **17/17 PASS in a single run, 45.3 s.** Includes the new `AppLockDeepLinkE2ETest.repeatedWrongPins_throttleEntry_evenForCorrectPin_andSurviveRelaunch` (D-24), the full Mentor journey (chat → resume → reflect → save once → history), provider failure recovery, 5 proactive-reminder tests, memory lifecycle, provider-switch consent, onboarding, journal save/reopen and the PIN-gated deep link |
| `:core:data:connectedDebugAndroidTest` | `com.stoicjournal.core.data.test` (self-contained test APK, synthetic DBs) | **11/11 PASS.** Migrations 3/4/5→6 + fresh install, reflection persistence idempotency (rollback, concurrency), Mentor persistence (D-34) and coordinator device tests. The first attempt hit `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: a stale copy of that *test-only* package was signed with QA-001's WSL debug key. Only that test APK was uninstalled (`:core:data:uninstallDebugAndroidTest`) |

Result: **28/28 instrumentation tests pass on the device in one clean run each**, plus **97/97 JVM tests**. This is a total of **125 automated tests, 0 failures, 0 skipped.** It replaces QA-001's caveat ("each test passed at least once; last runs had 2 environment failures").

Raw results: `app/build/outputs/androidTest-results/connected/qa/TEST-Pixel 10 Pro - 17-_app-.xml`, `core/data/build/outputs/androidTest-results/connected/debug/TEST-Pixel 10 Pro - 17-_core_data-.xml` (build outputs, not committed).

## 5. Not verified in QA-002 (stated plainly)
| Area | Reason | Status |
|---|---|---|
| Instrumentation on device | done later in the session, see §4b | **PASS (28/28)** |
| Screenshot/visual inspection (themes, large fonts, insets), manual lifecycle (rotation, process kill, network loss) | personal device; the agent shell (needed for `adb` screen capture and `am`/`svc` control) failed with `Couldn't create PTY` for the whole session | NOT_TESTED in QA-002 (QA-001 lifecycle evidence stands) |
| Release signing + `apksigner verify` + SHA-256 for QA-002 artefacts | no `RELEASE_*` credentials on this host; the agent shell tool failed (`Couldn't create PTY`) for the rest of the session | PENDING |
| Commit and push of the QA-002 changes | same shell failure | PENDING: see §6 |
| Live provider calls | no authorised test credentials | `LIVE_PROVIDER_VERIFICATION_PENDING` |
| Gemini HTTP-level mocking | the Google client SDK path cannot be mocked without replacing it | unchanged from QA-001 |

## 6. Exact owner/next-agent steps to close QA-002
```powershell
cd D:\dev-android\stoic-journal
git add -A app core feature docs/final-qa
git commit -m "fix(security,qa): PIN brute-force throttle (D-24), wire backup rules (D-38), isolate instrumentation in com.stoicjournal.qa (D-39), Wisdom Library tests (SJ-FINAL-QA-002)"
git push -u origin qa/final-qa-002
```
The device run is already green (§4b). Next, merge `qa/final-qa-002` into `integration/mentor-into-main` (non-force) and rebuild the signed release with `RELEASE_*` supplied from `~/.gradle/gradle.properties`.

## 7. Effect on the verdict
QA-002 finds no new P0 and adds one P1, D-39. D-39 is a test-safety defect: it harmed the owner's device data when tests ran, but it does not affect the shipped app. It is fixed in the build. The QA-001 blockers are unchanged: the exposed keystore credentials on public `main` must be rotated (D-37, owner), and the integration merge is the owner's decision. The verdict stays **`SJ_FINAL_INTEGRATION_BLOCKED`** (C). Code and tests are fully green: 125/125 automated tests, 28 of them on the device. What still blocks a release candidate is owner-only: keystore rotation, the signed build and the merge decision. The QA-002 commit is not yet made because the agent shell failed.
