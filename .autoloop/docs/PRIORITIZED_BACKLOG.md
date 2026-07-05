# Prioritized Backlog — stoic-journal

Updated 2026-07-05 (autoloop run 0001). One hypothesis per iteration from the top of "Now".

## Now (next 1–3 iterations)
- [ ] **Docs: standard-workstation build instructions** — `docs/build-environment.md` only documents the Termux/aarch64 host; README "Getting Started" predates this machine. Verify + document the standard path (JDK 17, ANDROID_HOME, API 34 auto-provisioning). *(selected for run 0001 — smallest, unblocks contributors, zero code risk)*
- [ ] **CI: add `testDebugUnitTest` to build.yml** — tests exist but CI only assembles; a PR can break tests silently (R-6).
- [ ] **CI: add `lintDebug`** — no static-analysis gate at all today.

## Next
- [ ] Gradle wrapper 8.7 → 8.14.x (staged toolchain refresh, step 1 of R-2)
- [ ] AGP 8.5.0 → latest 8.x (step 2; keep Kotlin 2.0 until AGP-9 run)
- [ ] target/compileSdk 34 → 35+ with behavior-change review (R-3)
- [ ] Keystore backup procedure + Play App Signing decision (R-4)
- [ ] Unit tests for feature ViewModels (start with `feature:today`) (R-1)

## Later
- [ ] AGP 9 migration (built-in Kotlin; drop standalone KGP) — single dedicated run
- [ ] Compose BOM refresh + Material3 API deltas
- [ ] Room DB encryption decision (R-5)
- [ ] Instrumentation tests in CI via android-emulator-runner
- [ ] Screenshots for README + store listing

## Done (moved here with run number)
