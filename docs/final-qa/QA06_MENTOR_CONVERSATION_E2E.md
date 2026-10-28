# QA06 — Stoic Mentor Conversation End-to-End

## 1. Journey under test (mission §8)
Open app → Today "AI Mentor" → Conversations → "+" → `MentorChat` (header shows the **active** provider) → send first message → streaming reply visible (streaming bubble) → send follow-up → second reply → Back (list shows the conversation titled with the first 30 chars) → reopen → both exchanges present → generate reflection (QA07).

## 2. Why it could not pass at 568cbc7
* D-01: model id `"default"` sent to every real provider → 404 / SDK error → `Failed(ProviderUnavailable)`.
* D-32: even with a correct model, OpenAI/Anthropic streaming crashed with a Flow-invariant violation after the first delta (reproduced by the new MockWebServer tests before the fix).
* D-02: the provider was always OpenAI regardless of which key the user had entered.
* D-13: resuming a conversation left stale collectors writing into the UI state.
The previous QA06 draft (prior session) reported this journey as `PASS` using the `FakeAiProvider`; with a real provider it was impossible. It is now verified with the fake provider **for UI/persistence** and with mocked HTTP **for the real providers' wire behaviour**; live providers remain `LIVE_PROVIDER_VERIFICATION_PENDING` (QA05 §4).

## 3. Required assertions → where verified

| Assertion | JVM | Device (`MentorJourneyE2ETest`) |
|---|---|---|
| Correct provider used | `DefaultAiProviderRegistryTest`, `MentorConversationCoordinatorTest` (conversation saved with active provider id) | header "Provider: FAKE_PROVIDER"; saved conversation `providerId == fake_provider` |
| Correct model used | coordinator & provider tests (`defaultModelId`, explicit id passthrough) | `fakeProvider.lastRequest.selectedModel == "fake-model-1"`; conversation `modelId == "fake-model-1"` |
| No duplicate user messages | coordinator test (persisted once, excluded from forwarded history); provider body tests (exactly one occurrence) | `onAllNodesWithText(userText).assertCountEquals(1)` |
| Streaming is visible | coordinator emits Started/TextDelta/Completed | streaming bubble text appears before completion (fake provider emits one delta) |
| Messages saved, order preserved | sequence numbers 1..4 | sequence numbers 1..4 after two exchanges |
| Conversation persists & can be resumed | — | back → list shows title → reopen → both replies present |
| Cancellation works | `OpenAiProviderTest.cancelling the collector cancels the HTTP call` (socket released) | "Stop Generation" button bound to `cancelActiveRequest` (UI); not exercised with the instant fake provider |
| Errors are recoverable | coordinator failure test (user message kept, no mentor message) | `providerFailure_isRecoverable_…`: switch to key-less Claude → "Reflection failed" snackbar → dismiss → switch back → continue same conversation (3 messages) |

## 4. Results

* `MentorJourneyE2ETest.fullMentorJourney_…` **PASS** on device (runs `app_mentor`, `app_e2e_final3`): provider header, two exchanges, sequence 1..4, resume from list, reflection generated/edited/saved once, visible in History.
* `MentorJourneyE2ETest.providerFailure_isRecoverable_…` **PASS** (`app_remaining`, `app_e2e_final`, `app_e2e_final2`).
* `MentorCoordinatorDeviceTest` 2/2 **PASS** — and it found **D-34** (every message cascade-deleted on conversation update) which the prior QA06 "PASS" could not have observed.
* JVM: `MentorConversationCoordinatorTest` 8/8.
