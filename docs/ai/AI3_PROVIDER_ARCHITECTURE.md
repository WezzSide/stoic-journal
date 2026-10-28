# AI-3 Multi-Provider Architecture Specification

## Overview
Stoic Journal features a production-ready, extensible multi-provider AI architecture designed to support OpenAI, Anthropic Claude, and Google Gemini while ensuring zero hardcoded keys or unapproved network leaks.

## Supported Providers
1. **OpenAI (`openai`)**: Implemented via OkHttp against `https://api.openai.com/v1/chat/completions`. Enforces `store: false` to ensure maximum user privacy and no provider-side logging/retention of personal entries.
2. **Anthropic Claude (`claude`)**: Implemented via OkHttp against `https://api.anthropic.com/v1/messages`. Enforces `anthropic-version: 2023-06-01` and supports event-based streaming (`content_block_delta`).
3. **Google Gemini (`gemini`)**: Implemented natively via Google Generative AI SDK (`com.google.ai.client.generativeai.GenerativeModel`), streaming content responses via `generateContentStream`.
4. **Fake Provider (`fake_provider`)**: Test provider used for deterministic UI and unit testing without network or quota expenditure.

## Architecture
- `AiProvider`: Interface declaring `validateConnection()`, `listAvailableModels()`, `generateResponse()`, `streamEvents()`, `cancelRequest()`, and `reportUsage()`.
- `AiProviderRegistry`: Thread-safe registry (`DefaultAiProviderRegistry`) that dynamically holds all configured providers and manages active selection.
- `SecureCredentialRepository`: Retrieves Keystore-encrypted credentials at request time so API keys are never stored in plaintext or in memory for long periods.
- `NetworkModule`: Injects a shared singleton `OkHttpClient` with 30-second timeouts.

## Streaming & Cancellation
- `AiStreamEvent`: Emits `Started`, `TextDelta`, `UsageUpdated`, `Completed`, and `Failed` events.
- `cancelRequest(requestId)`: Request-scoped cancellation via `OkHttpClient` Call cancellation or coroutine job cancellation.
