# QA07 — Reflection Engine (conversation → journal) End-to-End

## 1. Pipeline as traced

`MentorChatScreen` "Generate Reflection" → `MentorChatViewModel.generateReflection` → `ReflectionCandidateGenerator.generateCandidate` (non-streaming `generateResponse`, JSON-only system instruction, model id now resolved through the active provider — D-01) → markdown-fence stripping → `kotlinx.serialization` parse of seven string fields (`virtues` always empty; `whatHappened` parsed but not persisted/editable — P3) → `ReflectionCandidateCache` (in-memory, singleton) → navigate `mentor_reflection_preview/{candidateId}` → `MentorReflectionPreviewViewModel.loadCandidate` → user edits title/summary/insights/control/intention → `saveReflection` → `ReflectionPersistenceCoordinatorImpl.saveReflectionCandidate(candidate, "REVIEW_REQUIRED")` → `ReflectionCandidateValidator` → `db.withTransaction { getLink → (existing ? return entry) : insert journal_entries + insert mentor_reflection_links }` → `isSaved` → navigate Today (`popUpTo(0)`) → History lists the `GUIDED` entry → EntryDetail shows the composed body.

Save policy: the UI always uses `REVIEW_REQUIRED`; `MANUAL` / `AUTHORIZED_AUTO_SAVE` exist in the enum but no code path performs automatic saving — **no unsafe auto-save exists**.

## 2. Defects found and fixed in this area

| ID | Finding | Fix |
|---|---|---|
| D-01 | generator sent model `"default"` → every real provider failed, so no candidate could ever be produced | `resolveModelId` |
| D-17 | preview returned before `Scaffold` when the candidate was missing → blank screen with no message; candidate lost on process death (in-memory cache) | error state with "Back to conversation" rendered; cache remains in-memory (documented limitation, acceptable because the conversation itself is persisted and the user can regenerate) |
| — | duplicate-save / concurrency / rollback were **untested** (previous QA07 claimed idempotency "verified" without a test) | `ReflectionPersistenceIdempotencyTest` (instrumentation, in-memory Room) |

## 3. Destructive-condition simulations (mission §9) — `ReflectionPersistenceIdempotencyTest` (Room in-memory, never the user's DB)

| Simulation | Assertion | Result |
|---|---|---|
| Repeated submission (3 sequential saves, edited title on the 2nd, different policy on the 3rd) | same entry id returned, **1** row in `journal_entries` | _(see §5)_ |
| Duplicate callback / concurrent saves (25 parallel coroutines) | all succeed, all return the same id, **1** row | |
| Different candidates, same conversation | 2 rows (no over-deduplication) | |
| Candidate validation failure (blank title) | failure result, **0** rows | |
| Interrupted save (link DAO throws inside the transaction) | failure result, journal insert **rolled back** (0 rows); next healthy save creates exactly 1 | |
| Journal visibility | saved entity has title, composed body ("Within My Control…"), `virtues="WISDOM"` | |
| Process recreation | candidate cache is in-memory → preview shows "Reflection candidate not found." + back action (D-17); verified by code path and UI state test; no journal row is created | by design |

Acceptance invariant **"exactly one journal entry per (conversation, candidate) regardless of repeated save attempts"** is enforced by (a) the pre-check inside the transaction and (b) the unique index `index_mentor_reflection_links_conversationId_candidateId` (schema 5+, migration-tested in QA10).

## 4. On-device journey — `MentorJourneyE2ETest.fullMentorJourney_…` (Pixel 10 Pro, offline provider)

Generate → "Review Reflection" shows the generated summary → edit title → "Save to Journal" → Today → `JournalRepository` contains exactly one `GUIDED` entry with the edited title and the intention text → History shows it → EntryDetail shows the body.

## 5. Results

`ReflectionPersistenceIdempotencyTest` 6/6 **PASS** in three consecutive device runs (`core_data.raw.txt`): repeated saves → 1 row; 25 concurrent saves → 1 row, same id; different candidates → 2 rows; invalid → 0 rows; interrupted link insert → journal insert rolled back, next save creates exactly 1; saved body/virtues verified. On-device journey PASS (QA06). Acceptance invariant holds.
