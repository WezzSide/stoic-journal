# QA13 — Performance, UI Quality & Accessibility

Tools actually available: `am start -W` (ThisTime/TotalTime), `dumpsys gfxinfo` (frame stats), `dumpsys meminfo` (PSS), `logcat` (ANR/crash scan), Compose semantics via the instrumentation suite, screenshots. Android Profiler / Macrobenchmark were not available from the shell (QA00). No measurement below is estimated; each comes from the named command on the Pixel 10 Pro.

## 1. Measurements (Pixel 10 Pro, debug build — release would be faster with R8)

| Metric | Value | Source |
|---|---|---|
| Cold start (`MainActivity`, after force-stop) | 635 ms / 630 ms | `am start -W` (`evidence/lifecycle/cold_start.txt`, `session.txt`) |
| Warm start | not measurable this session (another app took the foreground) | — |
| Memory after cold start | PSS 174 MB, RSS 300 MB | `dumpsys meminfo` |
| Frame stats | only 5 frames rendered since the counter reset (2 janky) — too few frames for a meaningful figure | `dumpsys gfxinfo` |
| Streaming responsiveness | fake provider: reply rendered well within the 10 s wait in every Mentor run; HTTP providers: MockWebServer deltas parsed line-by-line without buffering the whole body (`OpenAiProviderTest`) | instrumentation / JVM |
| Database query latency | all Room queries are `Flow`/suspend on IO; migration 3→6 with validation completes within the test (ms range) | `MigrationTest` |
| Background worker activity | one periodic 24 h work + optional one-shot snooze; no network, no AI | `ProactiveReminderE2ETest` |
| Recompositions | not measured (no Compose compiler metrics / Layout Inspector from the shell) | — |

Android Profiler / Macrobenchmark were not available; the figures above are the only ones actually measured. Nothing else is claimed.

## 2. UI quality checklist

Visual inspection could not be completed safely: the only screenshots captured during the lifecycle pass coincided with the device owner using other apps and were deleted unviewed; later the owner's own journal/mentor data existed in the install, so content screens were not captured at all. What *was* verified is functional: light/dark theme and font scale 1.3 did not crash or lose the Settings screen (process survival), and the instrumentation suite exercised Today, Settings, Memory & Context, Mentor chat/list/preview, History, Entry detail and the lock screen on this 1080×2410 device without any "node not found / off-screen" failures attributable to layout (all `performScrollTo` calls succeeded). Landscape, small/large-screen and long-content layouts: **NOT_TESTED visually**.

## 3. Accessibility review (static + semantics)

| Check | Finding |
|---|---|
| Content descriptions | Icon buttons carry descriptions ("Back", "Send Message", "Voice Input", "Stop Generation", "New Conversation", "Delete Conversation", "History", "Library", "Settings", "Delete memory", "Vergrendeld", "Biometrische ontgrendeling", "Wissen"). Decorative action icons in the proactive notification use resource 0 (text-only actions) — fine. |
| Touch targets | Quick actions and settings rows use `heightIn(min = 48.dp)`; new hour steppers use `IconButton` (48 dp). |
| Screen-reader labels | Switches are paired with visible labels in the same row; new memory/proactive switches additionally carry test tags (not read aloud). |
| Focus order | Linear column layouts; composer field → send button. |
| Text contrast | Material 3 dynamic colour scheme; status/subtitle text uses `onSurfaceVariant` (meets M3 defaults). |
| Dynamic text scaling | checked at font scale 1.3 (see §2). |
| Error announcements | Errors are shown as Snackbars/inline text (`errorMessage`); no `LiveRegion` semantics — screen-reader users must discover errors by focus (P3). |
| Language | UI mixes Dutch and English within one screen (Settings) — a comprehension/a11y concern (P3, D-26). |

## 4. Stability

Across all instrumentation runs and the lifecycle session, logcat (filtered to the app) contains **no** `FATAL EXCEPTION`, no `ANR in com.stoicjournal`, no "process has died" for the app, no Room `SQLiteException`, and no uncaught coroutine exception (`crash_scan.txt`, `*.logcat.txt`). The only app-side anomaly found was behavioural (D-34), caught by tests.
