# AI Feature Architecture

## Overview
The AI Reflection feature in Stoic Journal follows Clean Architecture principles, ensuring decoupling from the Gemini SDK and providing a resilient, testable data path.

## Components

### Domain Layer (`:core:domain`)
- **Models**:
    - `AiReflectionRequest`: Encapsulates user input, theme, mood, and context.
    - `AiReflectionResponse`: Structured response containing insights, interpretations, questions, and next steps.
    - `AiReflectionTheme`: Enum for various Stoic reflection themes (Anxiety, Discipline, etc.).
- **Repositories**:
    - `AiRepository`: Interface defining the contract for getting AI reflections.
- **Use Cases**:
    - `GetStoicReflectionUseCase`: Orchestrates the request, builds prompts, and invokes the repository.
    - `SaveAiReflectionUseCase`: Handles saving the structured AI response into the user's journal history.

### Data Layer (`:core:data`)
- **Repositories**:
    - `GeminiAiRepositoryImpl`: Low-level implementation that interacts with the `google-generativeai` SDK. Uses a system instruction for persona and forces JSON output.
    - `SafeAiRepository`: A wrapper that handles exceptions, rate limits, and maps them to domain-safe results.
    - `CachedAiRepository`: Implements a Room-based caching strategy to provide offline access to previous reflections.
    - `FakeAiRepository`: Used for unit tests and local development/previews.
- **Caching**:
    - `AiReflectionEntity`: Room entity for storing cached reflections.
    - `AiReflectionDao`: Data access object for the cache.

### Feature Layer (`:feature:ai`)
- **ViewModel**: `AiReflectionViewModel` manages UI state (Idle, Loading, Success, Error) and handles user actions like generating reflections and saving to the journal.
- **UI**: `AiReflectionScreen` provides a Material 3 interface with theme selection (FilterChips), clear status indicators, and structured result cards.

## Prompt Engineering
The `StoicPromptTemplate` object in the domain layer centralizes prompt construction:
- **System Prompt**: Establishes the Stoic philosopher persona and rules.
- **User Prompt**: Combines user input with theme-specific context and constraints.
- **Output Format**: Enforces a strict JSON structure for predictable parsing.
