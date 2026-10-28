# QA00 — Tool Capability Inventory

Mission: SJ-FINAL-QA-001 · Worktree: `.claude/worktrees/final-qa` (branch `qa/final-qa-001`) · Start SHA: `568cbc7071e85b587a5ce084404e029778797fda` (contains mission SHA `712fe71` as ancestor)
Inventory date: 2026-10-28. Every status below was established by actually executing the tool, not by checking for an executable.

Status vocabulary: `FUNCTIONAL` (executed successfully), `CONFIGURATION_REQUIRED` (present, needed a change before it worked), `INSTALLED` (present, not exercised), `UNAVAILABLE`.

## Host

| Item | Observation |
|---|---|
| Shell host | WSL2 Ubuntu 24.04, kernel `6.18.54.1-microsoft-standard-WSL2`, x86_64 |
| Windows host | Android Studio `AI-261.23567.138.2611.15646644` installed at `/mnt/d/dev-android/android-studio` (product-info.json). Not driven from this session (no IDE automation surface available); used only as the source of the Windows SDK/AVD. |
| JDK (shell) | Temurin OpenJDK 21.0.12.1 (`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`). Gradle's daemon-JVM toolchain pins JDK 17 (`gradle/gradle-daemon-jvm.properties`, vendor Microsoft via foojay) and provisioned it automatically. |
| Gradle | Wrapper 8.13 (`gradle/wrapper/gradle-wrapper.properties`). `FUNCTIONAL` — `:app:assembleDebug` succeeded in 10m34s (see QA03). |
| AGP / Kotlin | AGP 8.13.2, Kotlin 2.0.21, Compose BOM 2024.06.00, Hilt 2.51.1, Room 2.6.1, WorkManager 2.9.0 (`gradle/libs.versions.toml`). |

## Capability matrix

| Capability | Tool | Available | Functional | Evidence |
|---|---|---|---|---|
| Project inspection | Android Studio IDE | INSTALLED (Windows) | NOT_EXERCISED | No IDE-automation MCP/plugin exposed to this session; inspection performed with git/grep/read on the worktree instead. |
| Build execution | Gradle 8.13 + AGP 8.13.2 | YES | FUNCTIONAL (after config) | First run failed: `Installed Build Tools revision 35.0.0 is corrupted` because `local.properties` pointed at the **Windows** SDK (`C:\Users\Admin\AppData\Local\Android\Sdk`), whose build-tools contain Windows binaries that AGP-on-Linux cannot execute. Fixed by pointing the worktree's `local.properties` at the WSL SDK `/home/Admin/Android/Sdk` and installing `platforms;android-34` with `sdkmanager` (it had only android-35/37). Second run: `BUILD SUCCESSFUL in 10m 34s`, 311 tasks. Logs: `evidence/logs/01_assembleDebug_failed.log`, `02_assembleDebug.log`. |
| Kotlin compiler | kotlinc via Gradle | YES | FUNCTIONAL | Same build; 12 warnings, all `Kapt currently doesn't support language version 2.0+. Falling back to 1.9.` |
| Unit tests (JVM) | Gradle `test` | YES | FUNCTIONAL | See QA03 / QA14. |
| Android Lint | Gradle `lintDebug`, SDK `cmdline-tools/latest/bin/lint` | YES | see QA03 | Prior session reported a lint crash in `ComposableCoroutineCreationDetector`; re-verified in QA03. |
| ADB (device control) | `adb` 1.0.41 (34.0.4, Debian) in WSL; `adb.exe` 36.x in Windows SDK platform-tools | YES | FUNCTIONAL via **Windows** adb only | WSL `adb devices -l` → empty list (WSL cannot see the Wi-Fi-paired device). `/mnt/c/.../platform-tools/adb.exe devices -l` → `adb-55211FDCH008GQ-N60HOw._adb-tls-connect._tcp device product:blazer model:Pixel_10_Pro`. All device commands in this mission use `adb.exe -s <that serial>`. |
| Physical device | Google Pixel 10 Pro (`blazer`) | YES | FUNCTIONAL | `ro.build.version.release=17`, `ro.build.version.sdk=37`, build `CP3A.261005.005`, user build, 1080x2410 @ 420dpi (override 356). **`com.stoicjournal` was NOT installed** at mission start (`pm list packages` → none), so installing test builds cannot touch personal journal data. |
| Android Emulator | `emulator.exe` (Windows SDK) + AVD `medium_phone` | INSTALLED | NOT_EXERCISED | WSL SDK has no `emulator/` and no system images. Windows SDK has system images for API 36 and 37 (google_apis_playstore x86_64) and one AVD. Launching the Windows emulator from WSL is possible via interop but was not required: the physical device covers API 37 and no API ≤34 image exists locally. Downloading additional images was deliberately avoided (mission §3.1). |
| UI automation | Compose UI test (`ui-test-junit4`), Espresso 3.6.1, Hilt testing | YES | FUNCTIONAL | Existing `app/src/androidTest` scaffolding (`HiltTestRunner`, `HiltTestActivity`, `NavigationTest`). New E2E suite added in QA14 and executed on the Pixel. |
| UI automation (ADB) | `adb.exe shell input tap/text/keyevent`, `uiautomator dump` | YES | FUNCTIONAL | Used for manual journeys and notification interaction (QA04/QA08). |
| Screenshot inspection | `adb.exe exec-out screencap -p` + image read | YES | FUNCTIONAL | Screenshots stored under `docs/final-qa/evidence/screenshots/` (synthetic data only). |
| Database inspection | `adb.exe shell run-as com.stoicjournal` + `sqlite3` (debug build) ; Room `MigrationTestHelper` | YES | FUNCTIONAL | Used in QA07/QA10. Android Studio Database Inspector not driven from the shell. |
| Logcat | `adb.exe logcat` | YES | FUNCTIONAL | Used for crash/ANR scanning. |
| Performance profiling | `adb.exe shell am start -W` (TotalTime), `dumpsys gfxinfo`, `dumpsys meminfo` | YES | FUNCTIONAL (basic) | Android Profiler / Macrobenchmark not available from shell; measurements limited to am/dumpsys counters (QA13). |
| Accessibility analysis | Compose semantics via UI tests; `uiautomator dump` for content-desc | PARTIAL | PARTIAL | No Accessibility Scanner app on device; checks limited to semantics/contentDescription/touch-target review (QA13). |
| Release tooling | `apksigner`, `zipalign`, `aapt2` in `/home/Admin/Android/Sdk/build-tools/35.0.0`; `bundletool` not installed | YES | FUNCTIONAL | Used in QA17. |
| GitHub integration | `gh` 2.x authenticated as `WezzSide` (scopes gist, read:org, repo, workflow) | YES | FUNCTIONAL | `gh pr view 1` succeeded. Remote `B0LK13/stoic-journal.git` redirects to `WezzSide/stoic-journal`. |
| Web research | WebSearch / WebFetch | YES | FUNCTIONAL | Used to validate current provider model identifiers (QA05). |

## MCP server discovery

The session's MCP surface was enumerated from the tool registry (no new servers were installed, per mission §2.2).

| Server | Status | Relevant to this mission? | Used |
|---|---|---|---|
| `codebase-memory-mcp` (index_repository, search_graph, trace_path, …) | Connected | Marginal — static code-graph queries; grep/read were sufficient for a 12k-line codebase | No |
| `claude-artifacts` / Claude Docs | Connected | No (document publishing) | No |
| `sharepoint-ssot` | **Failed to connect** (`CONNECTION_CLOSED`) | No | No |
| claude.ai connectors (Bird, Canva, Figma, Gmail, Google Drive/Docs/Sheets/Calendar, Hugging Face, Lovable, Lucid, Microsoft 365/Learn, Netlify, Notion, PubMed, Slack, Supabase, Vercel, Wix, Cloudflare) | Connected | No — none exposes Android Studio, ADB, device, emulator, UI-automation, screenshot or database capabilities | No |

**Conclusion:** no Android Studio MCP, Android device MCP, ADB MCP, mobile-automation MCP or screenshot/vision MCP is available. All device work is performed through the Windows-side `adb.exe` reached from WSL, and all UI automation through Compose/Hilt instrumentation tests plus ADB input injection.

## Plugin discovery

Android Studio plugins were not enumerable from the shell (no IDE process control). No agent IDE plugin exposes tools to this session. Trusted existing tooling (Gradle, SDK command-line tools, adb, gh) was prioritised; the only addition was the `platforms;android-34` SDK package, which the project's `compileSdk = 34` requires.

## Prior-session artefacts

The main checkout already contained an untracked `docs/final-qa/` with twelve 8–15-line files (QA00–QA11), each ending in `Verdict: PASS` and citing no logs, test output, screenshots or commands (e.g. QA04 claims UI automation "manually verified … on connected physical Pixel 10 Pro" with no artefacts; QA10 claims migrations "tested locally" while no migration test exists in the repo). They were treated as **unverified claims** and are superseded by the documents in this directory. Discrepancies between those claims and observed evidence are listed in `SJ_FINAL_ACCEPTANCE_REPORT.md`.
