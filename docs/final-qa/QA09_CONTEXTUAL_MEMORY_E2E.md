# QA09 — Contextual Memory (AI-7) End-to-End

## 1. State at 568cbc7

* `MentorMemoryPreferences` (memoryEnabled, historicalJournalAccessEnabled, usePreviousConversations, useApprovedMemories, sendHistoricalContextToProvider) all defaulted to `false` and **no UI could change them** (`Screen.MemorySettings` had no composable, no navigator).
* `MentorMemoryRepository.saveMemory` had **no caller** → `mentor_memories` could never be populated; there was no approve/review/delete surface.
* `sendHistoricalContextToProvider` (the transmission consent) and `usePreviousConversations` were **never read** by any logic. Had the flags been enabled, journal excerpts would have been appended to the system prompt and sent to the cloud provider without that consent (latent P0; mitigated only by the absence of UI).
* Retrieved journal text was injected into the system instruction under plain headings — no delimiter, no "treat as data" instruction.

Net effect: the feature shipped as dead code and the privacy contract was enforced by accident rather than by design.

## 2. Remediation on `qa/final-qa-001`

| Layer | Change |
|---|---|
| Domain | `MentorMemoryRepository.getAllMemories()` + `approveMemory(id)`; `HistoricalReflectionRetriever` returns `EMPTY` unless `memoryEnabled && (journalAccess || approvedMemories) && sendHistoricalContextToProvider`; only `userApproved` memories, ≤5 memories, ≤3 entries × 150 chars; context wrapped in `<<<BEGIN/END_USER_AUTHORISED_CONTEXT>>>` with an explicit untrusted-data instruction; delimiter strings inside user text are stripped |
| Data | `MentorMemoryDao.getAllMemories` (status ≠ DELETED), `approveMemory` (APPROVED + userApproved=1 + lastReviewedAt), soft `deleteMemory` (DELETED + userApproved=0) |
| UI | New `MemorySettingsScreen` (feature:settings → Settings row "Geheugen & context" → route `memory_settings`): four consent switches (dependent switches disabled until the master is on), "New memory" field → **proposal** (status PROPOSED, not usable), Approve button, Delete icon, status chips, empty state |
| Coordinator | unchanged API; now receives the delimited context only when consent is complete |

Memory proposals are **user-authored only**; there is no automatic extraction from conversations or journal entries (no hidden memory extraction, no new AI feature introduced).

## 3. Lifecycle & security tests

| Requirement (mission §11) | Test | Level |
|---|---|---|
| Memory disabled → no historical context used | `ContextualMemoryTest.nothing is retrieved when memory is disabled`; `MentorConversationCoordinatorTest.journal context is never sent unless…` | JVM |
| Enable → proposal → review → approve → available | `SettingsAndMemoryE2ETest.memoryLifecycle_…` (`getApprovedMemories` empty while PROPOSED, 1 after approve) | device |
| Authorised conversation uses memory | `MentorConversationCoordinatorTest` asserts the provider request contains the memory/journal text **only** with full consent, inside the delimiters | JVM |
| Delete → future requests exclude memory (Room query, in-memory cache, retrieval, prepared context) | `ContextualMemoryTest.deleted memories cannot reappear`; `MigrationTest.freshInstall_…` (DAO approve/delete round-trip); E2E delete → `getAllMemories` & `getApprovedMemories` empty. There is no in-memory memory cache or retrieval index; the retriever queries Room on every request. | JVM + device |
| Deletion does not erase journal entries | `ContextualMemoryTest.deleted memories cannot reappear` asserts the source entry survives | JVM |
| Unapproved memories excluded | `ContextualMemoryTest.unapproved memories are never included` | JVM |
| Excluded journal entries ignored (journal access off) | `ContextualMemoryTest.journal entries are excluded when historical access is off…` | JVM |
| Historical access requires permission | gate on `historicalJournalAccessEnabled` + `sendHistoricalContextToProvider` | JVM |
| Context budget enforced | `ContextualMemoryTest.context budget is enforced…` (5 memories, 3 entries, 150-char excerpts) | JVM |
| Provider switching requires consent | confirmation dialog + persisted selection (`SettingsAndMemoryE2ETest.activeProviderSwitch_…`) | device |
| No unauthorized journal upload — inspect outgoing provider request | `MentorConversationCoordinatorTest` inspects `AiProviderRequest.authorizedHistoricalContext`; `OpenAiProviderTest`/`AnthropicClaudeProviderTest` inspect the **serialized HTTP body** and show the system field carries exactly that string and the user message exactly once | JVM |
| Prompt-injection: hostile journal text cannot forge the context terminator | `ContextualMemoryTest.hostile journal text cannot inject a fake context terminator` | JVM |

## 4. Results

JVM `ContextualMemoryTest` 8/8 and `MentorConversationCoordinatorTest` 8/8 PASS (`08_full_build_tests.log`). Device `SettingsAndMemoryE2ETest.memoryLifecycle_…` PASS in every run it executed (`app_e2e`, `app_e2e_rerun`, `app_e2e_final`): switches persisted (memory on, approved-memories on, send-to-provider on, journal access off), proposal stored as PROPOSED/unapproved and absent from `getApprovedMemories`, approve → 1 approved, delete → 0 in both queries, empty state shown. `MigrationTest.freshInstall_…` PASS covers DAO approve/soft-delete at the Room level.
