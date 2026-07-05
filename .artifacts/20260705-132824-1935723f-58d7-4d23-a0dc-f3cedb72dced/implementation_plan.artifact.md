# Implementation Plan - Stoic Journal AI Hardening & MVP Preparation

This plan outlines the transformation of the Gemini-powered AI Reflection feature into a production-ready MVP component.

## Proposed Changes

### 1. Security & Configuration
- **Objective**: Ensure secrets are protected and production risks are documented.
- **Actions**:
    - Verify `.gitignore` covers `local.properties`.
    - Create `SECURITY_NOTES.md` to document the "embedded key" risk and recommend backend proxy solutions for production.

### 2. Architecture & Domain Layer
- **Objective**: Decouple from Gemini SDK and provide resilient data paths (Safe, Cached, Fake).
- **Files**:
    - `core:domain`:
        - [NEW] `AiReflectionModels.kt`: Define `AiReflectionRequest`, `AiReflectionResponse`, `AiFailureReason`, and `AiReflectionTheme`.
        - `AiRepository.kt`: Update interface to use new domain models.
    - `core:data`:
        - [NEW] `SafeAiRepositoryImpl.kt`: Wrapper for handling rate limits, network errors, and safety blocks.
        - [NEW] `CachedAiRepositoryImpl.kt`: Room-based or DataStore-based caching for offline access.
        - [NEW] `FakeAiRepository.kt`: Deterministic responses for tests and Compose Previews.
        - `GeminiAiRepositoryImpl.kt`: Refactor to focus strictly on SDK interaction and mapping.

### 3. Feature Layer (:feature:ai)
- **Objective**: Improve UX with structured inputs, theme selection, and "Save to Journal" capability.
- **Actions**:
    - Update `AiReflectionViewModel` to support theme selection and saving to history.
    - Refactor `AiReflectionScreen`:
        - Add Material 3 `FilterChip` for theme selection.
        - Implement distinct UI states (Idle, Loading, Success, Error).
        - Add "Save to Journal" and "Regenerate" buttons.

### 4. Testing & Verification
- **Objective**: Ensure 100% stability in error/offline scenarios.
- **Actions**:
    - Unit tests for `GetStoicReflectionUseCase` using `FakeAiRepository`.
    - ViewModel state transition tests.
    - Regression tests for missing API keys.
    - APK verification using `gradle assembleDebug` and Shizuku scripts.

## Verification Plan

### Automated Tests
- `./gradlew test` (Targeting core:domain and core:data)
- `./gradlew :feature:ai:test` (ViewModel logic)

### Manual Verification
- Launch app, navigate to AI Reflection.
- Test with "No Network" (Flight Mode) to verify `CachedAiRepository` or Error UI.
- Test with "Empty Input" to verify validation.
- Verify "Save to Journal" adds an entry to the History screen.
