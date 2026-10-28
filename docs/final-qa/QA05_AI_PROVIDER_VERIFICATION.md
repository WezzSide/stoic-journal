# QA05 — AI Provider Verification (OpenAI, Anthropic Claude, Google Gemini)

## 1. Model compatibility (mission §7.2) — validated against current provider documentation on 2026-10-28

| Provider | Shipped at 568cbc7 | Status per provider docs | Shipped now |
|---|---|---|---|
| OpenAI | runtime list from `/v1/models` (filter `gpt-`); fallback `gpt-4o`, `gpt-4o-mini`; **model actually sent: `"default"`** | `gpt-4o*` snapshots are deprecated (shutdown Jan 2027); current catalogue: `gpt-6-astra` (flagship), `gpt-6.1-sol`, `gpt-6.1-terra`, `gpt-6-luna` — developers.openai.com/api/docs/models, …/deprecations | default `gpt-6.1-sol`; known list `gpt-6.1-sol, gpt-6-astra, gpt-6.1-terra, gpt-6-luna`; live `/v1/models` still preferred when reachable |
| Anthropic | `claude-3-5-sonnet-latest` (default), `claude-3-5-haiku-latest` (validation ping), `claude-3-7-sonnet-latest`, `claude-3-opus-20240229` | **all retired**: 3.5 Sonnet 2025-10-28, 3 Opus 2026-01-05, 3.5 Haiku & 3.7 Sonnet 2026-02-19 → API requests fail | default `claude-opus-5-5`; list `claude-opus-5-5, claude-sonnet-5-5, claude-haiku-5-5`; validation ping uses `claude-haiku-5-5` (1 token) |
| Gemini | `gemini-2.0-flash` (default + validation), `gemini-1.5-pro`, `gemini-1.5-flash` | `gemini-2.0-flash` **shut down 2026-06-01** (replacement `gemini-3.6-flash`); 1.5 family no longer listed; docs recommend `gemini-3.8-flash` / `gemini-3.5-flash-lite` for new projects — ai.google.dev/gemini-api/docs/models, …/deprecations | default `gemini-3.8-flash`; list `gemini-3.8-flash, gemini-3.6-flash, gemini-3.5-flash-lite, gemini-3.1-pro-preview` |

Root cause of the P0 (D-01): `MentorConversationCoordinator` created every conversation with `modelId = "default"` and forwarded it verbatim; OpenAI sent `"model":"default"`, Anthropic/Gemini only substituted a real id for *blank* strings. No real provider could ever answer. Fix: `AiProvider.defaultModelId` + `resolveModelId()` (placeholder/blank → provider default) applied in the coordinator, the reflection generator and all providers; legacy rows with `"default"` are resolved at request time (unit-tested).

Sources: [OpenAI models](https://developers.openai.com/api/docs/models), [OpenAI deprecations](https://developers.openai.com/api/docs/deprecations), [Gemini models](https://ai.google.dev/gemini-api/docs/models), [Gemini deprecations](https://ai.google.dev/gemini-api/docs/deprecations), Anthropic model table from the bundled `claude-api` reference (cached 2026-10-06).

## 2. Mocked HTTP validation (mission §7.1)

Implemented with OkHttp `MockWebServer` (`core/data/src/test/.../OpenAiProviderTest.kt`, `AnthropicClaudeProviderTest.kt`, shared `ProviderTestSupport.kt`). The providers gained an `internal var baseUrl` so the public endpoint is replaced only in tests.

| Scenario | OpenAI | Claude | Gemini |
|---|---|---|---|
| Valid streaming response → Started / TextDelta×n / Completed, accumulated content | ✔ | ✔ | SDK-based; not mockable at HTTP level (see §4) |
| Serialized request body: model id (placeholder resolved), `stream`, `store:false`, system message first, history roles in order, current user message exactly once | ✔ | ✔ (`system` field, `max_tokens`, alternating roles) | — |
| Headers (`Authorization: Bearer` / `x-api-key` + `anthropic-version`) and path | ✔ | ✔ | — |
| Explicit model id passed through unchanged | ✔ | (covered by resolve test) | ✔ (unit) |
| Invalid credentials (401) → `InvalidCredentials` | ✔ | ✔ | — |
| Rate limit (429) → `RateLimited` | ✔ | ✔ | — |
| Unavailable model (404) → `ProviderUnavailable` | ✔ | — | — |
| Malformed SSE lines skipped, stream continues | ✔ | — | — |
| Missing credential → `MissingCredentials` **before any network call** | ✔ | — | — |
| Network timeout → recoverable `Failed(Unknown)` | ✔ (2 s read timeout, `NO_RESPONSE`) | — | — |
| Cancellation terminates the HTTP call (`activeCallCount()` 1 → 0) | ✔ | — | — |
| Non-streaming `generateResponse` content + usage parsing (used for reflection extraction) | ✔ | ✔ | — |
| Validation ping uses cheapest current model | — | ✔ | — |
| Retired model ids absent from `listAvailableModels` | — | ✔ | ✔ |

Results: see §5 (filled from the unit-test run).

## 3. Conversation correctness (mission §7.3)

Covered by `MentorConversationCoordinatorTest` (JVM) and `MentorJourneyE2ETest` (device, offline provider):
user message appears exactly once (persisted once, excluded from the forwarded history); system instruction travels separately from user content; roles preserved (`USER`/`MENTOR` → `user`/`assistant`, Gemini `user`/`model` via structured `startChat` history — D-20); model id valid; provider selection persisted in DataStore and re-applied after restart (`DefaultAiProviderRegistryTest`, `SettingsAndMemoryE2ETest`); switching requires an explicit confirmation dialog; streaming completes and persists the mentor message; failure leaves the user message and no mentor message; cancellation releases the socket.

## 4. Live provider testing (mission §7.4)

No test credentials and no authorisation for billable requests were available to this session. The only key found on the machine (`GEMINI_API_KEY` in the developer's `local.properties`) is a personal credential and was **not** used. Gemini's HTTP layer is hidden behind the `com.google.ai.client.generativeai` SDK (0.9.0, legacy line) and therefore has no MockWebServer coverage; its request shaping (`resolveModelId`, structured history, system instruction) is covered by code review and the registry unit tests only.

**Status: `LIVE_PROVIDER_VERIFICATION_PENDING`** for all three providers. Simulated responses were never presented as live success.

## 5. Results

`OpenAiProviderTest` 10/10, `AnthropicClaudeProviderTest` 5/5, `DefaultAiProviderRegistryTest` 6/6, `ProviderVerificationTest` 4/4 — all green in `evidence/logs/08_full_build_tests.log` (and every later run). Before the D-32 fix the four streaming tests **failed** (`06_build_after_fixes.log`), proving the suite detects the defect.

Unintended live observation (disclosed, not claimed as verification): while the owner had just stored a Claude key in the debug build on the device, two instrumentation tests that selected `claude` to exercise the *missing-key* path reached the real Anthropic API and received a streamed answer (model `claude-opus-5-5`, synthetic prompt "QA error path" / "QA no key"; `evidence/instrumentation/app_mentor.raw.txt`). The key was never read, logged or exported by the agent. The tests were corrected to use only a key-less provider. Status remains `LIVE_PROVIDER_VERIFICATION_PENDING`; the accidental call does show that the current model id and streaming parser work against the live Anthropic endpoint.
