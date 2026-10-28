# Acceptance Criteria — stoic-journal

Updated 2026-07-05 (autoloop run 0001). Every criterion maps to a command or gate.

| # | Criterion | How verified | Status 2026-07-05 |
|---|---|---|---|
| AC-1 | Clean checkout builds debug APK on standard workstation | `build` gate (`gradlew assembleDebug`) | ✅ run 0001: cold build + pre/post gates pass |
| AC-2 | Unit tests pass | `unit_tests` gate (`gradlew testDebugUnitTest`) | ✅ run 0001 pre/post gates pass |
| AC-3 | Android Lint reports no errors | `lint` gate (`gradlew lintDebug`, optional for now) | ✅ run 0001 pre/post gates pass |
| AC-4 | CI green on main | GitHub Actions `build.yml` badge | ✅ per README badge (verify with `gh run list`) |
| AC-5 | Instrumentation tests compile & pass on emulator | `gradlew connectedDebugAndroidTest` on Pixel8_API36 | ☐ not yet run on this host |
| AC-6 | APK installs and app launches on emulator | `adb install` + launch main activity | ☐ planned after first green build |
| AC-7 | Guided entry < 2 min (PRD success criterion) | manual on-device timing | ☐ |
| AC-8 | No secrets/keystore in git history | `git log --all --diff-filter=A -- '*.keystore'` + gitignore check | ✅ checked 2026-07-05 (ignored, absent) |
