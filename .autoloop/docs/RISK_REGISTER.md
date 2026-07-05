# Risk Register — stoic-journal

Updated 2026-07-05 (autoloop run 0001). Owned by Security Auditor + Reviewer.

| ID | Risk | Likelihood | Impact | Severity | Mitigation | Status |
|---|---|---|---|---|---|---|
| R-1 | Loop/feature changes regress behavior not covered by tests (ViewModels/UI untested) | H | M | **High** | expand unit tests per feature in future runs; instrumentation tests exist but aren't executed regularly | Open |
| R-2 | Stale toolchain (AGP 8.5.0 / Kotlin 2.0.0 / Gradle 8.7 / BOM 2024.06) accumulates upgrade debt; AGP 9 migration includes built-in-Kotlin breaking change | M | H | **High** | staged single-hypothesis upgrade runs: Gradle → AGP 8.x latest → SDK 35/36 → AGP 9 | Open |
| R-3 | targetSdk 34 will fall below Google Play's target-API requirement (Play requires within 1 year of latest Android; API 37 is current) | H | H | **High** | bump target/compileSdk in a dedicated run with behavior-change review (permissions, notifications) | Open |
| R-4 | Release keystore handling: `stoic-journal-release.keystore` referenced from `app/build.gradle.kts`, gitignored (`*.keystore`) — loss of the local file = loss of app identity | M | H | **High** | back up keystore + credentials to a password manager / offline vault; consider Play App Signing | Open |
| R-5 | Journal entries are sensitive personal data; DB currently unencrypted (standard Room/SQLite) | M | M | Medium | evaluate SQLCipher/EncryptedFile or documented accept-risk for MVP (device-local only) | Open |
| R-6 | CI builds debug APK only; unit tests/lint not enforced on PRs | M | M | Medium | extend `build.yml` with `testDebugUnitTest` + `lintDebug` (backlog) | Open |
| R-7 | Single-maintainer bus factor; private repo without branch protection | M | M | Medium | enable branch protection on main; document release process | Open |
| R-8 | Termux-era docs (`build-environment.md` AAPT2 override) can misconfigure standard hosts if copied blindly | L | M | Low | standard-workstation instructions added and override explicitly limited to Termux/aarch64 in run 0001 | Mitigated |

Security review notes (2026-07-05, clone-time scan): no secrets in repo (keystore correctly ignored), no network permissions expected in offline MVP — verify `AndroidManifest.xml` permissions in a future run.
