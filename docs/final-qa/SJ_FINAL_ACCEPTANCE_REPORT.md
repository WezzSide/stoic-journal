STOIC JOURNAL
FINAL ANDROID ENGINEERING ACCEPTANCE

Repository: WezzSide/stoic-journal (remote URL `B0LK13/stoic-journal.git` redirects; `gh` user WezzSide)
Branch: feature/stoic-mentor-multiprovider (= PR #1 head `sync/2026-10-27-stoic-journal-icon`, both at 568cbc7) → QA work on `qa/final-qa-001`; integration candidate `integration/mentor-into-main`
Starting SHA: 712fe71730ac68906b39fcb72a3e63d6d1a013c1 (mission) / 568cbc7071e85b587a5ce084404e029778797fda (branch HEAD at start, one docs commit later)
Final SHA (code): 326d1241 (`qa/final-qa-001`, last code/test commit) · 489cd78b84c9f7fc4699bfb98f0b35b968ad2bee (`integration/mentor-into-main`, release build); evidence-document commits follow these (see QA16 §4)
Android Studio Version: AI-261.23567.138.2611.15646644 (installed on the Windows host; not driven from the shell)
Android SDK: WSL `/home/Admin/Android/Sdk` — build-tools 35.0.0, platforms android-34/35/37 (compileSdk 34, minSdk 26, targetSdk 34); Gradle 8.13, AGP 8.13.2, Kotlin 2.0.21, JDK 17 daemon
Physical Device: Google Pixel 10 Pro (blazer), Android 17 / API 37, build CP3A.261005.005 — the owner's personal phone, reached via wireless debugging through the Windows `adb.exe`
Emulators Tested: none (no API ≤34 system image available locally; Windows AVD `medium_phone` API 36 not used; no downloads performed)

MCP Servers Discovered: codebase-memory-mcp (connected), claude-artifacts / Claude Docs (connected), sharepoint-ssot (connection failed), plus claude.ai productivity connectors (Bird, Canva, Figma, Gmail, Google Drive/Docs/Sheets/Calendar, Hugging Face, Lovable, Lucid, Microsoft 365/Learn, Netlify, Notion, PubMed, Slack, Supabase, Vercel, Wix, Cloudflare). No Android Studio / ADB / device / mobile-automation / screenshot MCP exists.
MCP Tools Successfully Used: none required (all device work via ADB + instrumentation; web research via WebSearch/WebFetch)

Total Tests Executed: 115 distinct tests in the final suites (88 JVM unit + 11 core:data instrumentation + 16 app instrumentation); 6 instrumentation runs of the app suite in total
Tests Passed: 115 distinct tests have a passing run on the final application code (88/88 JVM; 27/27 instrumentation passed at least once on the device)
Tests Failed: 0 attributable to the application in the final code; the last app-suite runs show 2 environment-caused failures (device dozing / in personal use) — see QA04 §4–5
Tests Skipped: 0 (`providerFailure` / keyless-provider tests Assume-skip only if all three real providers hold credentials; they ran)

P0 Defects Found: 4 (D-01 model id "default" sent to every provider; D-32 Flow-invariant crash in OpenAI/Claude streaming; D-34 conversation update cascade-deleted all Mentor messages; D-37 release signing credentials committed in plain text on public `origin/main`)
P0 Defects Resolved: 3 in code + D-37 removed from the integration candidate
P0 Defects Remaining: 0 in code; D-37 requires the owner to rotate the exposed keystore/passwords (history rewrite not permitted to the agent)

P1 Defects Found: 7 (D-02 provider selection never applied; D-03 proactive mentor unreachable; D-04 notification deep link/Reflect Now missing; D-05 contextual memory inert; D-06 legacy flag re-arms evening reminder; D-07 Snooze = Skip; D-08 release signing default passwords)
P1 Defects Resolved: 7 (D-08 fixed in the build; keystore rotation is an owner action)
P1 Defects Remaining: 0 (owner action outstanding for D-08: regenerate/rotate the release keystore)

Critical E2E Journeys: E2E-001…E2E-018 PASS on device/JVM (E2E-003 "edit saved entry" NOT_APPLICABLE — not a product feature; E2E-020 theme/accessibility visual checks NOT_TESTED on this device)
Database Migration Status: PASS — 3→6, 4→6, 5→6 and fresh install validated on device against exported schemas; no destructive migration; D-34 fixed
Provider Verification Status: MOCKED_HTTP_VERIFIED (OpenAI, Claude) + MODEL_IDS_VALIDATED (all three, 2026-10-28 docs) · LIVE_PROVIDER_VERIFICATION_PENDING (no authorised test credentials; one unintended live Claude observation disclosed)
Privacy & Security Status: PASS with owner actions — consent gate enforced, no journal transmission without explicit flags, credentials Keystore-encrypted, lock gate holds against deep links, secrets plugin removed; OPEN hardening items D-24, D-30, D-31 (P2/P3); owner must rotate the weak-password keystore
Performance Status: PARTIAL — cold start 630–635 ms (debug), PSS 174 MB, no crash/ANR; frame/recomposition metrics not measurable with available tools
Accessibility Status: PARTIAL — semantics/labels/touch targets reviewed statically and exercised by the suite; visual contrast/large-font inspection NOT_TESTED (screenshots unavailable on the in-use device)

Release APK: `app-release.apk` 3,877,349 bytes, signed (APK Signature Scheme v2, `apksigner verify` → Verifies), from integration candidate 489cd78
Release AAB: `app-release.aab` 7,383,513 bytes, JAR-signed, from integration candidate 489cd78
Artifact SHA-256: APK `1bd263944c72ce83a2780a3c5027935d97061eb585c77d15eccb2985afa154e9` · AAB `3199cb0bef2a05dbcb8cf9765ab205b9ee1ce4ae46dacc1473fe84fca486fb75`

GitHub PR Status: PR #1 (`sync/…` → `main`) is CONFLICTING and unchanged by this mission. New: draft PR from `qa/final-qa-001` and the conflict-free integration candidate `integration/mentor-into-main` (both pushed) — see QA16 §4 for URLs
Remaining Owner Actions: (0) **rotate the keystore whose passwords are committed on public main (D-37) and purge them from history**; (1) rotate/regenerate the local default-password release keystore and supply RELEASE_* secrets via ~/.gradle or CI secrets (D-08); (2) review `apks/*.apk` committed on main (D-36); (3) decide whether to merge the integration candidate and close/retarget PR #1; (4) authorise live provider verification with test keys, or accept LIVE_PROVIDER_VERIFICATION_PENDING; (5) check the Anthropic usage log for the 3–4 unintended small requests made with the key stored on the device (QA12 §9); (6) optional: migrate off the deprecated Gemini client SDK (Firebase AI Logic), harden PIN hashing (D-24)

FINAL VERDICT: SJ_FINAL_INTEGRATION_BLOCKED

---

## 1. Why this verdict and not A, B or D

* **VERDICT C** (`INTEGRATION_BLOCKED`) applies because **signing is unresolved and only the owner can resolve it**: (a) `origin/main` publishes the release signing passwords in plain text in a public repository (D-37) — the keystore they protect must be rotated and the leaked values purged, which requires history rewriting the mission forbids the agent to do; (b) the keystore available locally opens with the default password (D-08); (c) PR #1 is still CONFLICTING and the conflict-free candidate needs the owner's review/merge decision.
* **Not VERDICT A** (`RELEASE_CANDIDATE_VERIFIED`): the above, plus live provider verification requires authorised test credentials (not available) and visual UI-quality checks could not be completed on the only device.
* **Not VERDICT B** (`EXTERNAL_VALIDATION_PENDING`): its conditions (all local/automated/device tests passed, only external paid validation pending) are met for the *code*, but B presumes signing is in order; here the signing material itself is the blocker, so C is the more accurate verdict.
* **Not VERDICT D** (`REMEDIATION_REQUIRED`): every P0/P1 *code* defect is fixed and proven by tests that failed before the fix and pass after; no mandatory gate is violated by the candidate's code.

## 2. Mandatory engineering gates (mission §22)

| Gate | Status | Evidence |
|---|---|---|
| No unresolved P0 defects | PASS | QA15 (D-01, D-32, D-34 FIXED) |
| No unresolved release-blocking P1 defects | PASS | QA15 |
| No destructive database migrations | PASS | QA10; origin/main's `fallbackToDestructiveMigration()` rejected in the merge |
| No unauthorized AI transmission | PASS (code) · one *unintended* live call during testing disclosed | QA09, QA12 §9, `MentorConversationCoordinatorTest`, provider body tests |
| No exposed credentials | **FAIL on `origin/main`** (D-37: signing passwords committed, public repo) → removed in the candidate; **OWNER_ACTION** to rotate. App runtime: PASS (Keystore AES-GCM, noBackup, secrets plugin removed, no key in logs/evidence) | QA12 §7b |
| All release-critical navigation routes work | PASS | QA02 §3, NavigationTest + E2E suite; MemorySettings added; Insights not a release feature |
| Journal persistence works | PASS | JournalAndOnboardingE2ETest, MigrationTest |
| Reflection persistence is transactionally safe | PASS | ReflectionPersistenceIdempotencyTest (rollback, 25× concurrency) |
| Proactive reminders behave correctly | PASS | ProactiveReminderE2ETest 5/5 ×3, evaluator/timing unit tests |
| Memory permissions are enforced | PASS | ContextualMemoryTest 8/8, memory E2E |
| Critical end-to-end tests pass | PASS (each at least once on device; see caveat QA04 §5) | QA14 §3 |
| Release APK and AAB build successfully | PASS | QA17 (`14_release_integration.log`) |
| Release artifacts are verified | PASS (signature verifies; signed with a keystore that must be rotated — D-08) | QA17 |
| Repository integration state is documented | PASS | QA16 |

## 3. Acceptance dashboard

| Domain | Implementation | Automated Tests | Device Tests | Verdict |
|---|---|---|---|---|
| Onboarding | PASS | PASS (E2E-001) | PASS | PASS |
| Today Dashboard | PASS | PASS (NavigationTest, entry points) | PASS | PASS |
| Journaling | PASS (edit of saved entries not a feature) | PASS (E2E-002) | PASS | PASS |
| Journal History | PASS | PASS (reopen, reflection visible) | PASS | PASS |
| Wisdom Library | PASS (unchanged) | NOT_TESTED (no dedicated journey) | NOT_TESTED | NOT_TESTED |
| Settings | PASS | PASS (proactive, provider, lock prefs) | PASS | PASS |
| OpenAI | PASS (fixed D-01/D-32) | PASS (MockWebServer 10) | NOT_APPLICABLE (no key) | PASS (mocked) / live PENDING |
| Claude | PASS (fixed D-01/D-19/D-32) | PASS (MockWebServer 5) | unintended live observation only | PASS (mocked) / live PENDING |
| Gemini | PASS (fixed D-01/D-19/D-20) | PASS (registry/model tests; SDK path not mockable) | NOT_TESTED | PASS (static) / live PENDING |
| Stoic Mentor Chat | PASS (D-01, D-02, D-13, D-32, D-34 fixed) | PASS (coordinator 8, device coordinator 2) | PASS (fullMentorJourney, providerFailure) | PASS |
| Reflection Generation | PASS | PASS | PASS | PASS |
| Reflection Persistence | PASS | PASS (idempotency 6) | PASS | PASS |
| Proactive Reminders | PASS (D-03/04/07/14/15 fixed) | PASS (16 unit + 5 device) | PASS | PASS |
| Contextual Memory | PASS (D-05/D-09/D-21 fixed, UI added) | PASS (8 unit + E2E) | PASS | PASS |
| Privacy & Security | PASS with OPEN hardening items | PASS (consent, lock, provider body assertions) | PASS (lock gate) | PASS (owner actions: keystore) |
| Database Migrations | PASS | PASS (MigrationTest 4) | PASS | PASS |
| Performance | PARTIAL | NOT_APPLICABLE | PARTIAL (cold start, memory, no crash) | NOT_TESTED (full) |
| GitHub Integration | PASS (candidate built + unit-tested) | PASS (unit tests on candidate) | NOT_APPLICABLE | PASS (merge is owner's call) |

## 4. Discrepancies between prior claims and evidence (mission §22 independent verification)

| Prior claim (untracked `docs/final-qa/` from an earlier session, all "PASS") | Evidence found |
|---|---|
| QA04 "Stoic Mentor UI automation manually verified … on connected physical Pixel 10 Pro" | Impossible with the then-shipped toolchain: androidx.test 1.6 cannot inject input on this device (D-35); no artefacts existed |
| QA06 "Dispatched user message, received streamed TextDeltas … message history preserved" | With any real provider the model id was `"default"` (D-01), streaming crashed (D-32), and every conversation update erased its messages (D-34) — history could not have been preserved |
| QA07 "Double save attempts return existing JournalEntry" | No test existed; now proven by `ReflectionPersistenceIdempotencyTest` (sequential + concurrent + rollback) |
| QA08 Snooze "correctly mutated snoozedUntilTimestamp" | True but the reminder never returned after the snooze (D-07); no UI could enable the feature at all (D-03) |
| QA09 "strict privacy boundary … retrieval returns empty contexts" | True only because no UI existed; the transmission-consent flag was never read (D-09) |
| QA10 "Migrations 1->6 successfully implemented and tested locally" | No migration test existed; `MigrationTest` added and run on device |
| QA02 "All composables … map to navigation endpoints" | `MemorySettings` / `Insights` routes had no screens; `AiReflection` screen unreachable |
| QA03 "`./gradlew test`: SUCCESS" | True (39 tests) — but none covered the Mentor/provider/proactive/memory paths |

## 5. Incident disclosure
See QA12 §9 / QA04 §4(6): the device owner stored a Claude API key in the debug build during the session; two tests selecting the `claude` provider to test the missing-key path made live Anthropic calls with it (synthetic text; key never read by the agent). Tests were corrected to use only key-less providers and to restore the owner's provider selection. During the lifecycle pass other apps were in the foreground; screenshots taken then were deleted unviewed and logcat evidence was filtered to the app's own lines.

## 6. Evidence package index
QA00 tool inventory · QA01 environment · QA02 architecture · QA03 build & lint · QA04 UI automation · QA05 providers · QA06 Mentor E2E · QA07 reflection · QA08 proactive · QA09 memory · QA10 migrations · QA11 lifecycle · QA12 security · QA13 performance/accessibility · QA14 regression results · QA15 defect register · QA16 GitHub integration · QA17 release artefacts · `evidence/logs/*` (Gradle), `evidence/instrumentation/*` (device, app-filtered), `evidence/lifecycle/*`.
