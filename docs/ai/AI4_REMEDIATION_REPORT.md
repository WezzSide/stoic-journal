# AI-4 Remediation Report (AI5-0)

## Remediation Items Addressed

1. **Navigation Integration**:
   - Registered `Screen.MentorChat` (`mentor_chat/{conversationId}`) and `Screen.MentorConversationList` (`mentor_conversations`) in `Screen.kt`.
   - Wired composable routes in `StoicJournalNavHost.kt` connecting `TodayScreen` quick-action "AI Mentor" button and `SettingsScreen` settings.
   - Verified active rendering of chat and conversation list screens.

2. **Elimination of Duplicate User Messages**:
   - Refactored `MentorConversationCoordinator.kt` to ensure user messages are appended to the conversation history once, avoiding duplicate turns in provider HTTP payloads.

3. **Provider & Model Selection Persistence**:
   - Updated `SettingsViewModel.kt` to bind selected API provider and API key directly through `SecureCredentialRepository`.
   - Updated `DefaultAiProviderRegistry` to track active provider IDs safely.
   - Removed generic `"default"` string fallbacks from request builders.

4. **Request-Scoped Cancellation**:
   - Verified correlation ID mapping (`requestCorrelationId`) in `OpenAiProvider`, `AnthropicClaudeProvider`, and `GeminiAiProvider`.
   - Calling `cancelRequest(requestId)` invokes `okhttp3.Call.cancel()` or cancels active flow collection, releasing network sockets cleanly.

5. **Privacy Authorization**:
   - Enforced explicit permission gates before sending messages.
   - `store: false` privacy parameter enforced for OpenAI requests.
   - Privacy-first backup default (`android:allowBackup="false"`) preserved.

## Verification
- **Gradle Tasks**: `./gradlew assembleDebug test` — **`BUILD SUCCESSFUL`**
- **Unit Tests**: 100% PASS across `:core:data`, `:core:domain`, `:feature:ai`.
- **Verdict**: `AI4_REMEDIATION_VERIFIED`
