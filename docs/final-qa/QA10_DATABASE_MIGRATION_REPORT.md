# QA10 — Database & Migration Report

Room schema version **6**, `exportSchema = true`, exported JSON for versions 3, 4, 5, 6 in `core/data/schemas/com.stoicjournal.core.data.database.StoicJournalDatabase/` (versions 1 and 2 were never exported; their migrations exist but cannot be replayed from JSON).

## 1. Migration inventory

| Migration | Content | Destructive? |
|---|---|---|
| 1→2 | create `ai_reflections` | no |
| 2→3 | `journal_entries` + `virtues TEXT NOT NULL DEFAULT ''` | no |
| 1→3 | both of the above | no |
| 3→4 | create `mentor_conversations`, `mentor_messages` (FK cascade → conversations, 2 indexes), `mentor_sessions` (FK cascade, index) | no |
| 4→5 | create `mentor_reflection_links` + **unique index** `(conversationId, candidateId)` | no |
| 5→6 | create `mentor_memories` | no |

`fallbackToDestructiveMigration()` is **not** used (it *is* used on `origin/main` — see QA16; the integration candidate keeps the explicit migrations). The migration objects were moved from an anonymous block in `DataModule.provideDatabase` into `StoicJournalMigrations.ALL` so they can be unit-/instrumentation-tested; the SQL is byte-identical to the shipped version and matches the `createSql` in the exported JSON (checked by `MigrationTestHelper.runMigrationsAndValidate`, which fails on any entity/schema mismatch).

## 2. Tests — `core/data/src/androidTest/.../MigrationTest.kt` (Pixel 10 Pro, dedicated file `qa_migration_test.db`)

| Path | Synthetic data | Assertions |
|---|---|---|
| 3 → 6 | 2 journal entries | both preserved (id, body, virtues), all five mentor tables exist and are empty, schema validated against 6.json |
| 4 → 6 | 1 journal entry, 1 conversation (`modelId="default"` legacy value), 1 message | conversation + message preserved; `PRAGMA foreign_keys=ON` + delete conversation cascades to messages; journal untouched |
| 5 → 6 | 1 journal entry, 1 reflection link | link preserved; inserting a second link for the same (conversation, candidate) raises `SQLiteConstraintException` (duplicate prevention survives); `mentor_memories` created empty |
| Fresh install | — | opens at version 6; `MentorMemoryDao` propose → approve → soft-delete round-trip; deleted memory hidden from both queries but row retained |
| Reopen / restart | covered implicitly: `runMigrationsAndValidate` reopens the file with the real `StoicJournalDatabase` and validates; the E2E suite restarts the app process between test classes against the same on-device DB |

Transaction rollback is covered in QA07 (`ReflectionPersistenceIdempotencyTest.interruptedSave_rollsBackJournalEntry`).

## 3. Entity ↔ schema parity
11 `@Entity` classes in `StoicJournalDatabase` ↔ 11 tables in `6.json`; verified by the Room validation step above and by the `:core:data:kaptDebugKotlin` schema export (a drift would regenerate `6.json` and show up in `git status` — it did not).

## 4. Results

`MigrationTest` 4/4 and `MentorConversationPersistenceTest` 1/1 **PASS** on the Pixel in three consecutive runs (`evidence/instrumentation/core_data.raw.txt`). All paths 3→6, 4→6, 5→6 validated against the exported schema; synthetic journal entries, conversations, messages and reflection links preserved; FK cascade and the unique link index intact; fresh install opens at version 6. Additional finding fixed during the mission: **D-34** — `@Insert(onConflict = REPLACE)` on `mentor_conversations` (REPLACE = delete + insert) cascaded to `mentor_messages`, erasing a conversation's messages on every update; both Mentor DAOs now use `@Upsert`. No destructive migration exists on the QA/integration branches (origin/main's `fallbackToDestructiveMigration()` was rejected in the merge).
