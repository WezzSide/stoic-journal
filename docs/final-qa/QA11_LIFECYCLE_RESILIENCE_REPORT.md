# QA11 — Android Lifecycle & Resilience

Device: Pixel 10 Pro, Android 17 (API 37), debug build from `qa/final-qa-001`. All checks use synthetic data; device settings changed for a check (rotation lock, night mode, font scale) were read first and restored afterwards.

## 1. Scenario matrix

| Scenario | Method | Expected | Result |
|---|---|---|---|
| Application backgrounded / restored | `input keyevent HOME` → relaunch via `am start -n com.stoicjournal/.MainActivity` | same screen/state, no crash | see §2 |
| Activity recreated (rotation) | `settings put system accelerometer_rotation 0; user_rotation 1` → back to 0 | content survives, no crash | |
| Process killed and restarted | `am force-stop com.stoicjournal` (test build only, synthetic data) → relaunch | onboarding state, conversations, journal entries, provider selection persist (DataStore/Room) | |
| System dark mode changed | `cmd uimode night yes` / `no` | theme follows, no crash | |
| Font scale increased | `settings put system font_scale 1.3` → 1.0 | no clipped controls on Today/Settings | |
| Device locked / unlocked | `input keyevent KEYCODE_SLEEP` → `KEYCODE_WAKEUP` + `wm dismiss-keyguard` | app re-locks after timeout when app-lock enabled (unit: `MainViewModel`); AppLock E2E covers PIN gate | |
| Notification received while locked | worker run while screen off → `dumpsys notification` shows invitation | notification present with static text | |
| Low-memory conditions | `am send-trim-memory com.stoicjournal RUNNING_CRITICAL` while in Mentor chat | no crash, state kept | |
| Battery restrictions | not changed on the owner's device (would alter their power settings); WorkManager periodic work is subject to standard Doze — documented | — | NOT_TESTED |
| Network disconnected / restored during streaming | **BLOCKED** on this device: it is reached over wireless debugging, so disabling Wi-Fi/airplane mode would drop the test channel; it is also the owner's primary phone (mission §13 forbids disruptive network tests there). Covered instead by `OpenAiProviderTest.network timeout…` (recoverable `Failed`), `…cancelling the collector…`, and the coordinator failure test (user message kept, no duplicate). | — | BLOCKED (emulator with network controls not available locally) |

## 2. Observations (`evidence/lifecycle/session.txt`)

* Cold start of `MainActivity` after `force-stop`: `TotalTime` **635 ms** and **630 ms** (two measurements, `am start -W`).
* App process (pid 6302) survived dark-mode switch, font scale 1.3, forced landscape, settings restore, `send-trim-memory RUNNING_CRITICAL`, and screen sleep/wake — same pid throughout; no crash, no ANR, no "process died" in logcat (`crash_scan.txt` empty).
* Process kill + relaunch: app relaunched cleanly (630 ms); persisted state (onboarding complete, provider selection, memory/proactive preferences, Room data) is asserted by the instrumentation suite rather than by this session.
* Memory after relaunch: TOTAL PSS **174 MB**, RSS 300 MB, Dalvik heap ~6.5 MB used (`meminfo.txt`).
* **Caveat**: during this session the device owner brought another app to the foreground, so the UI-state checks (which screen is restored after background/warm start) and the screenshots could not be attributed to Stoic Journal; the warm-start figure (0 ms, activity not brought forward) is invalid and the screenshots were deleted unviewed. Rotation/dark-mode/font-scale rendering was therefore verified only by process survival here and by the themed Settings/Memory screens rendered during the instrumentation runs, not by visual inspection.
* Device settings were restored to their original values (night=auto, font_scale=0.85, accelerometer_rotation=1, user_rotation=0) — confirmed at the end of the script.

## 3. Verdict
Lifecycle resilience: PASS for process survival across configuration changes, trim-memory, lock/unlock and process restart (no crash); state persistence PASS via the automated suite; network-disruption scenario BLOCKED (see §1); visual restore checks NOT_TESTED on this device for the reason above.
