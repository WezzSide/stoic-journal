# QA08 — Proactive Stoic Mentor Reminders (AI-6) End-to-End

## 1. State at 568cbc7

The whole chain was unreachable and partly broken (see QA02 §4/§5a and QA15 D-03, D-04, D-07, D-14, D-15):

* no Settings control existed → `ProactiveMentorWorker` was never enqueued (default `proactiveMentorEnabled=false`);
* reminder time and quiet hours had no UI (`setPreferredReminderTime` / `setQuietHours` had zero callers);
* the notification carried `putExtra("route","mentor_chat")` but `MainActivity` never read it and `mentor_chat` is not a route → tap opened Today;
* there was no "Reflect Now" action, only Snooze/Skip;
* Snooze set `snoozedUntil = now+30 min` and then re-armed the 24-hour periodic work for the *next day*, so Snooze behaved exactly like Skip Today;
* notification id 2002 collided with the journal direct-reply confirmation;
* the receiver's coroutine had no exception handling (a DataStore error would crash the process).

## 2. Remediation on `qa/final-qa-001`

| Area | Change |
|---|---|
| Settings UI | New "Proactieve Stoic Mentor" section: enable switch (`ProactiveMentorSwitch`), reminder time row → Material time picker (`ProactiveTimeRow`), quiet-hours start/end steppers (`QuietStart±`, `QuietEnd±`). Explanatory copy states: local notification only, no background AI, no journal text. |
| ViewModel | `setProactiveReminderTime`, `setProactiveQuietHours`; enable/disable also (re)schedules and clears the snooze wake-up. |
| Scheduler | `ProactiveReminderTiming` (pure, unit-tested) computes delays; new `scheduleSnoozeWakeup` enqueues a unique one-time `ProactiveMentorWorker` at `snoozedUntil`; `cancelPendingInvitations` cancels both unique works. |
| Worker | "Reflect Now" action (same content intent), `NotificationRoutes.EXTRA_ROUTE`, id **2003**, `CATEGORY_REMINDER`, `VISIBILITY_PUBLIC` (static text only). |
| Receiver | Snooze → `setSnoozedUntil` + snooze wake-up + periodic re-arm; Skip → `skippedTodayDate`, clears snooze; both wrapped in try/catch, `goAsync` always finished. |
| Shell | `MainActivity` reads the route extra (cold start + `onNewIntent`) and hands it to `StoicJournalNavHost` **after** the lock gate; NavHost navigates to the conversation list and opens a new Mentor chat. |
| Legacy evening reminder | `DailyReminderWorker` no longer ORs the always-true `reminderEnabled`; `MainViewModel`/`BootCompletedReceiver` re-arm morning/evening only when their own switches are on (D-06). |

Snooze duration is fixed at 30 minutes (no UI for it) — documented limitation.

## 3. Simulated-time matrix (mission §10.3) — `ProactiveReflectionEligibilityEvaluatorTest`, `ProactiveReminderTimingTest` (JVM, injected clock/zone `Europe/Amsterdam`)

| Scenario | Expected | Test |
|---|---|---|
| Disabled | `Disabled` | ✔ |
| Before reminder time (12:00) | `Eligible` | ✔ |
| At reminder time (20:30) | `Eligible` | ✔ |
| After reminder time inside quiet hours (23:15) | `QuietHours` | ✔ |
| Quiet hours wrapping midnight (02:00, 07:59 quiet; 08:00 eligible) | ✔ | ✔ |
| Non-wrapping quiet window | ✔ | ✔ |
| Completed reflection today → suppressed; next day eligible | ✔ | ✔ |
| Snoozed until 21:00 → suppressed at 20:45, eligible at 21:00 | ✔ | ✔ |
| Skip Today → suppressed until local midnight, eligible next morning | ✔ | ✔ |
| DST fall-back night (2026-10-25) evaluates on local wall-clock | ✔ | ✔ |
| Delay to next reminder: same day / tomorrow / exactly-at → tomorrow / DST night = 24 h 30 min | ✔ | ✔ |
| Snooze delay = remaining window; expired snooze clamps to 1 min | ✔ | ✔ |

## 4. Device verification (Pixel 10 Pro, Android 17) — `ProactiveReminderE2ETest`, `SettingsAndMemoryE2ETest`, `AppLockDeepLinkE2ETest`
`ProactiveReminderE2ETest` **5/5 PASS** in three consecutive runs (`app_e2e_final`, `app_e2e`, `app_remaining`): notification id 2003 posted with title "A moment for reflection 🏛️", body without any synthetic journal marker, actions exactly ["Reflect Now", "Snooze 30m", "Skip Today"]; Snooze → `snoozedUntil` ≈ now+30 min, notification cancelled, one-time wake-up work ENQUEUED, worker re-run posts nothing; Skip Today → `skippedTodayDate` = today, nothing re-posted; completed GUIDED reflection today → nothing posted; disabling cancels the periodic work and posts nothing. `SettingsAndMemoryE2ETest.proactiveMentor_…` PASS (switch, quiet-hour steppers 23/7, time picker, persisted). `AppLockDeepLinkE2ETest` PASS in every run: with PIN lock on, launching MainActivity with the Reflect-Now route shows only the lock screen (no "Stoic Mentor"/"Conversations" nodes), wrong PIN → error text, correct PIN → Mentor chat opens.

## 5. Critical restrictions

* **No background AI inference / no paid background requests**: `ProactiveMentorWorker` only evaluates local preferences + Room and posts a static notification; no provider is touched (code path verified; no `AiProvider` dependency in the worker).
* **No personal journal text in notifications**: title/body are constants; asserted on device (`extras["android.text"]` contains no synthetic journal marker).
