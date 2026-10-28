# QA02 — Code & Architecture Audit

Scope: full static read of commit `568cbc7` (all 12 modules, 199 Kotlin files, ~12k lines) by two independent read-only reviewers (architecture/dead-code and security) plus the lead's own trace of every Mentor/AI execution path. Defect IDs (`D-xx`) are tracked in `QA15_DEFECT_REGISTER.md`; "Status" reflects the state at the end of the mission.

## 1. Module dependency rules

| Rule | Result | Evidence |
|---|---|---|
| `feature:*` depend only on `core:domain` / `core:common` | **Violation**: `feature:ai` declared `implementation(project(":core:data"))` (unused in sources) | `feature/ai/build.gradle.kts:19` → removed (D-18) |
| No `com.stoicjournal.core.data` imports outside `core:data`/`app` | PASS | grep: only `app/MainViewModel.kt` (AssetSeedImporter) |
| No feature→feature imports | PASS | grep of `import com.stoicjournal.feature.` inside `feature/` yields only intra-module |
| `feature:settings` / `feature:onboarding` → `core:notifications` | Allowed but undocumented (schedulers) | build files |
| `core:domain` has Android deps | `androidx.core.ktx`, `hilt.android` contradict CLAUDE.md "no Android deps" | `core/domain/build.gradle.kts:13-16` (INFO, unchanged) |

## 2. Hilt wiring

All domain interfaces have exactly one binding; no missing-binding or duplicate-binding failures were found (confirmed by the successful Hilt/kapt compile of `app`). Bindings: 16 `@Binds` + `AiRepository` `@Provides` in `DataModule.kt`, `SecureCredentialRepository`/`CredentialEncryption`/credential `DataStore` in `SecurityModule.kt`, `@ApplicationScope CoroutineScope` in `CommonModule.kt`. Added by this mission: `AiProviderSelectionRepository` → `AiProviderSelectionRepositoryImpl`.

Dead `@Inject` classes never injected in `src/main` (P3, left in place): `ReflectionEligibilityEvaluator`, `GetWeeklySummaryUseCase`, `GetStreakInsightsUseCase`, `SearchJournalEntriesUseCase`, `MentorReflectionLinkRepository(+Impl)`, `ExportUtils`. `MentorSessionEntity` is in the schema but has no DAO and is never written.

## 3. Navigation

| Route (`Screen.kt`) | Composable in NavHost | Navigated to | Verdict |
|---|---|---|---|
| Onboarding, Today, GuidedJournal, FreeFormJournal, ExerciseJournal, Library, QuoteDetail, History, EntryDetail, Settings | yes | yes | PASS |
| AiReflection (legacy Gemini screen) | yes | **no** (Today's "AI Mentor" goes to `MentorConversationList`) | dead surface; would fail anyway (`GeminiAiRepositoryImpl` uses `apiKey = "dummy_key"`) — D-22 |
| MentorChat, MentorConversationList, MentorReflectionPreview | yes | yes | PASS |
| MemorySettings | **no** → **added** (`MemorySettingsScreen`, entry row in Settings) | **no** → **yes** | D-05 fixed |
| Insights | no | no | not implemented (D-28, documented; `Screen.Insights` left as placeholder) |

Null-argument guards for the four parameterised routes log and pop (`StoicJournalNavHost.kt`). Notification deep link: before the fix `MainActivity` never read the Intent and the extra `"route"="mentor_chat"` matched no route (D-04); now `NotificationRoutes.EXTRA_ROUTE` is consumed by `MainActivity` (incl. `onNewIntent`) and handed to the NavHost **inside** the lock gate.

## 4. Dead / unreachable functionality (release-scoped)

| Item | Finding | Status |
|---|---|---|
| Proactive Stoic Mentor (AI-6) | No UI control called `SettingsViewModel.setProactiveMentorEnabled`; `ProactiveMentorWorker`/receiver chain could never run; `setPreferredReminderTime`/`setQuietHours` had zero callers | D-03 fixed: Settings section (switch, time picker, quiet-hour steppers) |
| Contextual memory (AI-7) | All five `MentorMemoryPreferences` flags default false with no UI; `saveMemory` never called → `mentor_memories` never populated; `sendHistoricalContextToProvider` and `usePreviousConversations` never read | D-05/D-09 fixed: `MemorySettingsScreen` (toggles, propose/approve/delete), retriever gated on consent; `usePreviousConversations` still unused (INFO) |
| Provider selection | `DefaultAiProviderRegistry.setActiveProvider` had zero callers; Settings chips only chose where to store a key | D-02 fixed |
| `DailyQuoteWidgetReceiver` | Declared in no manifest on this branch (origin/main declares it) | D-23 → resolved in integration candidate |
| `ProactiveMentorCard` composable | never composed | INFO |
| `AiProvider.cancelRequest` | never called from UI; cancel only cancelled the coroutine | D-12 fixed (socket released in `finally`) |

## 5. Execution-path traces (post-remediation)

**5a Proactive reminder.** Settings switch (`ProactiveMentorSwitch`) → `SettingsViewModel.setProactiveMentorEnabled` → `ProactiveMentorPreferencesRepositoryImpl` (DataStore `proactive_mentor_preferences`) → `ProactiveMentorScheduler.scheduleNextInvitation` (unique 24h periodic, delay from `ProactiveReminderTiming`) → `ProactiveMentorWorker.doWork` (re-evaluates `ProactiveReflectionEligibilityEvaluator`) → notification id 2003 with Reflect Now / Snooze 30m / Skip Today → `ProactiveMentorNotificationReceiver` (snooze → `scheduleSnoozeWakeup` one-time work; skip → `skippedTodayDate`) → tap → `MainActivity` extra → lock gate → `StoicJournalNavHost` → `MentorConversationList` + new `MentorChat`. Verified by `ProactiveReminderE2ETest`, `AppLockDeepLinkE2ETest`, `ProactiveReflectionEligibilityEvaluatorTest`, `ProactiveReminderTimingTest`.

**5b Mentor chat.** `MentorComposer` → `MentorChatViewModel.sendMessage` → `MentorConversationCoordinator.sendMessage`: active provider from persisted selection → conversation created with `provider.defaultModelId` → user message persisted once → `provider.streamEvents` with `resolveModelId(conversation.modelId)` (legacy `"default"` rows resolved) → deltas → `Completed` persists mentor message and updates conversation provider/model. Verified by `MentorConversationCoordinatorTest`, `OpenAiProviderTest`, `AnthropicClaudeProviderTest`, `MentorJourneyE2ETest`.

**5c Reflection.** "Generate Reflection" → `ReflectionCandidateGenerator` (model resolved) → `ReflectionCandidateCache` (in-memory) → `MentorReflectionPreviewScreen` (now renders an error state when the candidate is gone) → `ReflectionPersistenceCoordinatorImpl` (`db.withTransaction`, link lookup + unique index) → `journal_entries` → History. Verified by `ReflectionPersistenceIdempotencyTest` (sequential, 25× concurrent, rollback, validation) and `MentorJourneyE2ETest`.

## 6. Coroutine / Flow hygiene

* `MentorChatViewModel.loadConversation` launched new collectors on every call without cancelling the previous ones and `init` bound a throw-away UUID (D-13) → fixed with `Job` fields and a no-op guard.
* Streaming providers: `withContext(IO)` + `emit` inside `flow { }.flowOn(IO)` — legal (same dispatcher) and exercised by the MockWebServer tests; `Response`/`BufferedReader` were never closed and `CancellationException` was swallowed into a `Failed` event (D-12) → `use {}` + rethrow + `call.cancel()` in `finally`.
* `ProactiveMentorNotificationReceiver` used an unscoped `CoroutineScope(IO)` with `try/finally` only (D-15) → exceptions now caught and logged; `goAsync` always finished.
* No `GlobalScope`, `runBlocking` or main-thread network in `src/main`. All screens use `collectAsState()` rather than `collectAsStateWithLifecycle()` (P3, unchanged except the new memory screen).

## 7. Other inconsistencies

* Room: 11 entities ↔ 11 tables in `schemas/…/6.json`; migrations 1→2, 2→3, 1→3, 3→4, 4→5, 5→6 match the exported `createSql`; no destructive fallback. Migrations were moved from an anonymous block inside `DataModule.provideDatabase` to `StoicJournalMigrations` so they can be tested (behaviour unchanged).
* `conversationMode` is hard-coded `"DAILY_REFLECTION"` on save regardless of the chosen chip; `ReflectionCandidate.whatHappened` is neither editable nor persisted; `virtues` always empty from the generator (P3).
* `versionCode = 1` / `versionName = "1.0"` hard-coded in the application convention plugin.
* UI copy mixes Dutch (Settings, reminders, lock screen) and English (Mentor, Today, Memory).
* `Class.forName("com.stoicjournal.MainActivity")` in the worker is safe under R8 (manifest-kept) but fragile.

## Verdict

Architecture is sound where it is wired (DI, Room schema parity, navigation guards, transactional persistence), but at `568cbc7` three of the four AI milestones (provider selection, proactive mentor, contextual memory) were **not reachable from the UI** and the one that was (Mentor chat) **could not succeed against any real provider** (D-01). All of these are remediated on `qa/final-qa-001`; residual items are P3/INFO and listed in QA15.
