# Task 5 Report: Implement use cases in core:domain

## Status: BLOCKED

## What was implemented
- Created 6 use case files under `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/`:
  - `GetTodayPromptUseCase.kt`
  - `GetQuoteForPromptUseCase.kt`
  - `SaveJournalEntryUseCase.kt`
  - `GetJournalEntriesUseCase.kt`
  - `ToggleFavoriteQuoteUseCase.kt`
  - `GetStreakUseCase.kt`
- Verified that `core/domain/build.gradle.kts` already includes Hilt dependencies.
- Verified that repository interfaces already live in `core/domain/.../repository/` and `DataModule.kt` already imports them from the domain package.

## What was tested
Ran:
```bash
./gradlew :core:domain:assembleDebug :core:data:assembleDebug
```

Result: **BUILD FAILED**

```
e: file:///root/projects/stoic-journal/core/domain/src/main/java/com/stoicjournal/core/domain/usecase/GetStreakUseCase.kt:3:30 Unresolved reference 'data'.
e: file:///root/projects/stoic-journal/core/domain/src/main/java/com/stoicjournal/core/domain/usecase/GetStreakUseCase.kt:14:49 Unresolved reference 'StreakCalculator'.
```

## Blocking issue
The task brief specifies `GetStreakUseCase` to import `com.stoicjournal.core.data.sync.StreakCalculator`. However:
- `core:data` already declares `implementation(project(":core:domain"))`.
- `core:domain` does not declare a dependency on `core:data`.
- Adding one would create a circular module dependency.

Therefore, `core:domain` cannot reference `core:data.sync.StreakCalculator` as written.

## Possible resolutions
1. Move `StreakCalculator` (and any other shared pure logic) into `core:domain` or `core:common` so the domain layer can use it without depending on `core:data`.
2. Inline the streak calculation directly in `GetStreakUseCase`.
3. Define a `StreakCalculator` interface in `core:domain` and bind an implementation from `core:data` via Hilt.

I did not apply any of these changes unilaterally because the brief explicitly shows the `core:data.sync` import and instructs not to restructure outside the task.

## Files changed
- `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/GetTodayPromptUseCase.kt` (created)
- `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/GetQuoteForPromptUseCase.kt` (created)
- `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/SaveJournalEntryUseCase.kt` (created)
- `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/GetJournalEntriesUseCase.kt` (created)
- `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/ToggleFavoriteQuoteUseCase.kt` (created)
- `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/GetStreakUseCase.kt` (created)

## Self-review findings
- 5 of 6 use cases compile in isolation.
- The remaining use case violates module dependency direction and cannot compile as specified.

## Next step
Need guidance on how to resolve the `GetStreakUseCase` dependency on `core:data.sync.StreakCalculator`.

---

## Resolution: Circular dependency fixed

### What changed
- Moved `StreakCalculator` from `:core:data` to `:core:domain`:
  - `core/domain/src/main/java/com/stoicjournal/core/domain/util/StreakCalculator.kt`
  - `core/domain/src/test/java/com/stoicjournal/core/domain/util/StreakCalculatorTest.kt`
- Updated package declarations to `com.stoicjournal.core.domain.util`.
- Deleted the old `:core:data` files in `core/data/src/main/java/com/stoicjournal/core/data/sync/` and `core/data/src/test/java/com/stoicjournal/core/data/sync/`.
- Updated `GetStreakUseCase` to import `com.stoicjournal.core.domain.util.StreakCalculator`.
- Added `testImplementation(libs.junit)` to `core/domain/build.gradle.kts` so the relocated unit test compiles.
- Committed all Task 5 changes (the six use cases plus the `StreakCalculator` relocation).

### Test results
```bash
./gradlew :core:domain:assembleDebug :core:data:assembleDebug
```
Result: **BUILD SUCCESSFUL**

```bash
./gradlew :core:domain:testDebugUnitTest
```
Result: **BUILD SUCCESSFUL** — `StreakCalculatorTest` passes.

### Commits
- `9bee959` — fix(core:domain): move StreakCalculator to domain layer to resolve circular dependency
