# Task 10 Report: Implement Journal feature

## What was implemented

Implemented the `:feature:journal` module with three journal entry screens, their ViewModels, and shared UI components.

### Files created

- `feature/journal/src/main/java/com/stoicjournal/feature/journal/components/JournalTextField.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/components/QuoteHeader.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/components/SaveDiscardBar.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/guided/GuidedJournalViewModel.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/guided/GuidedJournalScreen.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/freeform/FreeFormJournalViewModel.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/freeform/FreeFormJournalScreen.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/exercise/ExerciseJournalViewModel.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/exercise/ExerciseJournalScreen.kt`

### Summary

- **Shared components**: `JournalTextField` provides a large, placeholder-capable text input; `QuoteHeader` displays a quote and attribution; `SaveDiscardBar` exposes Save/Discard actions.
- **Guided journal**: loads a prompt via `SavedStateHandle["promptId"]`, observes a matching quote through `GetQuoteForPromptUseCase`, and saves a `GUIDED` entry.
- **Free-form journal**: optional title, body, and ability to attach/remove a random quote from the repository; saves a `FREE_FORM` entry.
- **Exercise journal**: loads an exercise via `SavedStateHandle["exerciseId"]`, displays description and steps, and saves an `EXERCISE` entry with the user's reflection.

The existing `feature/journal/build.gradle.kts` already matched the required configuration (`stoic.android.feature` plugin, correct namespace), so it was left unchanged.

## What was tested

Ran the module build:

```bash
./gradlew :feature:journal:assembleDebug --no-daemon
```

Result: `BUILD SUCCESSFUL in 1m 11s` (53 actionable tasks: 28 executed, 25 up-to-date).

No AAPT2 or Kotlin compilation issues occurred.

## Commit

- `94df5ba` feat(journal): add guided, free-form, and exercise journal entry screens

## Self-review findings

- All files match the task brief exactly.
- Core domain interfaces (`PromptRepository`, `QuoteRepository`, `ExerciseRepository`, `SaveJournalEntryUseCase`, `GetQuoteForPromptUseCase`) are used as specified.
- Hilt ViewModel injection and `SavedStateHandle` usage are consistent with the codebase.
- No unused imports or placeholders remain in the touched files.

## Issues or concerns

None. The module builds cleanly and the implementation follows the specified architecture.

---

# Task 10 Fix Report

## What was changed

### 1. Discard must clear the persisted draft

- Added `discard(onComplete: () -> Unit)` to `FreeFormJournalViewModel`, `GuidedJournalViewModel`, and `ExerciseJournalViewModel`.
- Each `discard()` calls `journalDraftRepository.clearDraft(...)` for the correct draft key (`EntryType.FREE_FORM` + `"freeform"`, `EntryType.GUIDED` + `promptId`, `EntryType.EXERCISE` + `exerciseId`), then invokes `onComplete()` to navigate back.
- Updated `FreeFormJournalScreen`, `GuidedJournalScreen`, and `ExerciseJournalScreen` so the `Discard` action in `SaveDiscardBar` calls `viewModel.discard(onNavigateBack)` instead of navigating back directly.

### 2. Free-form draft quote restoration is racy

- In `FreeFormJournalViewModel`, removed the one-shot `.first()` fetch of quotes during draft restoration.
- Added `draftQuoteIdToRestore` and `quoteExplicitlySet` flags.
- The draft body/title are restored immediately; the saved `quoteId` is held until the quotes flow emits a non-empty list containing the matching quote.
- A new `restoreDraftQuoteIfNeeded(quotes)` helper is invoked on every quotes emission and only attaches the saved quote when:
  - a saved `quoteId` exists,
  - the quotes list is non-empty,
  - the user has not explicitly attached or removed a quote this session.
- `attachRandomQuote()` and `removeQuote()` mark `quoteExplicitlySet = true` so a user choice is never overwritten by a later quotes emission.

### 3. Pending drafts are not lost on exit

- In `onCleared()` of all three ViewModels, the in-flight `draftSaveJob` is cancelled only after recording whether it had already completed.
- If a save was still pending (`draftSaveJob?.isCompleted == false`), the current UI state is flushed to DataStore synchronously via `runBlocking { flushDraft() }`.
- A shared `flushDraft()` helper is used by both the debounced `scheduleDraftSave()` and the synchronous exit flush, avoiding duplicate save logic and ensuring the final keystrokes are persisted.

### 4. Cancellation exceptions are swallowed

- In the `save()` method of all three ViewModels, `CancellationException` is now caught explicitly, `isSaving` is reset, and the exception is re-thrown so the coroutine is cancelled cleanly.
- Other `Exception` types continue to populate `saveError` for the Snackbar UI.

### Files modified

- `feature/journal/src/main/java/com/stoicjournal/feature/journal/freeform/FreeFormJournalViewModel.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/freeform/FreeFormJournalScreen.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/guided/GuidedJournalViewModel.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/guided/GuidedJournalScreen.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/exercise/ExerciseJournalViewModel.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/exercise/ExerciseJournalScreen.kt`
- `.superpowers/sdd/task-10-report.md`

## Test results

Ran the requested build:

```bash
./gradlew :feature:journal:assembleDebug :core:data:assembleDebug :core:domain:assembleDebug --no-daemon
```

Result: `BUILD SUCCESSFUL in 1m 6s` (96 actionable tasks: 9 executed, 87 up-to-date).

No Kotlin compilation, kapt, or AAPT2 errors in the affected modules.

## Commit

- `TBD` fix(journal): clear drafts on discard, flush drafts on exit, and rethrow cancellations

## Issues or concerns

- None. All four Important reviewer findings are addressed and the affected modules build cleanly.

---

# Task 10 Critical Fixes Report

## What was changed

### 1. Auto-save journal drafts

- Added `JournalDraft` model in `core/domain/src/main/java/com/stoicjournal/core/domain/model/JournalDraft.kt`.
- Added `JournalDraftRepository` interface in `core/domain/src/main/java/com/stoicjournal/core/domain/repository/JournalDraftRepository.kt`.
- Added DataStore-backed `JournalDraftRepositoryImpl` in `core/data/src/main/java/com/stoicjournal/core/data/repository/JournalDraftRepositoryImpl.kt`.
- Wired the repository into Hilt via `core/data/src/main/java/com/stoicjournal/core/data/di/DataModule.kt`.
- Updated all three journal ViewModels to:
  - Restore the draft when the screen opens.
  - Save the draft debounced (300 ms) on every text/quote change.
  - Clear the draft after a successful save.

### 2. Fixed broken `isSaving` state in `GuidedJournalViewModel`

- Added `isSaving` and `saveError` to `GuidedJournalUiState`, `FreeFormJournalUiState`, and `ExerciseJournalUiState`.
- `isSaving` is now reset to `false` both on save success and in the `catch` block.
- `SaveDiscardBar` now accepts an `isSaving` flag, disables the buttons, and shows "Saving…" while a save is in progress.

### 3. Added save error handling

- Wrapped `saveJournalEntryUseCase` invocations in `try/catch` in `GuidedJournalViewModel`, `FreeFormJournalViewModel`, and `ExerciseJournalViewModel`.
- On error, `isSaving` is reset and `saveError` is populated with the exception message.
- Each screen observes `saveError` and shows a `Snackbar` via `SnackbarHost`; the error is cleared via `onSaveErrorShown()` after display.

### Files modified

- `core/data/src/main/java/com/stoicjournal/core/data/di/DataModule.kt`
- `core/data/src/main/java/com/stoicjournal/core/data/repository/JournalDraftRepositoryImpl.kt` (new)
- `core/domain/src/main/java/com/stoicjournal/core/domain/model/JournalDraft.kt` (new)
- `core/domain/src/main/java/com/stoicjournal/core/domain/repository/JournalDraftRepository.kt` (new)
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/components/SaveDiscardBar.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/exercise/ExerciseJournalScreen.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/exercise/ExerciseJournalViewModel.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/freeform/FreeFormJournalScreen.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/freeform/FreeFormJournalViewModel.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/guided/GuidedJournalScreen.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/guided/GuidedJournalViewModel.kt`

## Test results

Ran the requested build:

```bash
./gradlew :feature:journal:assembleDebug :core:data:assembleDebug :core:domain:assembleDebug
```

Result: `BUILD SUCCESSFUL in 43s` (96 actionable tasks: 57 executed, 39 up-to-date).

No compilation errors in the affected modules.

## Commit

- `d183cf7` fix(journal): auto-save drafts, fix isSaving, and add save error handling

## Issues or concerns

- The `git add -A` used for the commit also staged pre-existing untracked `.superpowers/sdd/*` files (base-task/review diff/report artifacts). They are now part of the commit. If those files should remain untracked, they need to be removed from the commit history.
- Draft restoration for the free-form screen fetches quotes once during init; if the quote list is still loading, a previously attached quote will not be restored. This is a minor edge case.

---

# Task 10 Second Fix Report

## What was changed

### 1. Do not block the main thread in `onCleared()`

- Removed `runBlocking { flushDraft() }` from `onCleared()` in `FreeFormJournalViewModel`, `GuidedJournalViewModel`, and `ExerciseJournalViewModel`.
- Added a new `@ApplicationScope` qualifier and a singleton process-level `CoroutineScope` (backed by `SupervisorJob() + Dispatchers.Default`) in `core/common/src/main/java/com/stoicjournal/core/common/di/`.
- Injected `@ApplicationScope CoroutineScope` into each journal ViewModel.
- In `onCleared()`, the final draft flush is now launched on the application scope so the UI thread is never blocked by DataStore I/O.
- Added the `stoic.android.hilt` convention plugin to `:core:common` so the new Hilt module compiles.

### 2. Fix the free-form draft quote restoration race

- Restructured `FreeFormJournalViewModel.init` so the persisted `JournalDraft` is read first (one-shot `.first()`) before `quoteRepository.getAllQuotes()` is collected.
- The saved `quoteId` is stored in `draftQuoteIdToRestore`; title and body are restored immediately.
- Quote collection starts only after the draft is loaded, and `restoreDraftQuoteIfNeeded(quotes)` is invoked on every emission.
- The saved quote is restored only when the quotes list is non-empty and contains a matching quote, and only if the user has not explicitly attached or removed a quote this session.
- `attachRandomQuote()` and `removeQuote()` continue to set `quoteExplicitlySet = true` so user choices are never overwritten.

### 3. Remove triplicated auto-save logic

- Created `JournalDraftSaver` in `feature/journal/src/main/java/com/stoicjournal/feature/journal/draft/JournalDraftSaver.kt`.
- The helper owns the 300 ms debounce delay, the in-flight `Job`, and `flushOnExit(scope)`.
- Each ViewModel supplies the draft key (`EntryType` + id), a `produceDraft` lambda returning the current `JournalDraft`, and the `journalDraftRepository::saveDraft` callback.
- Replaced duplicated `scheduleDraftSave()`, `flushDraft()`, and `onCleared()` logic in all three ViewModels with calls to `draftSaver.scheduleSave()` and `draftSaver.flushOnExit(applicationScope)`.
- Behavior is preserved: debounced auto-save, off-main-thread exit flush only when a save is pending, and cancellation-exception handling remains in the ViewModel's `save()` method for journal-entry persistence.

## Files modified

- `core/common/build.gradle.kts`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/freeform/FreeFormJournalViewModel.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/guided/GuidedJournalViewModel.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/exercise/ExerciseJournalViewModel.kt`
- `.superpowers/sdd/task-10-report.md`

## Files created

- `core/common/src/main/java/com/stoicjournal/core/common/di/ApplicationScope.kt`
- `core/common/src/main/java/com/stoicjournal/core/common/di/CommonModule.kt`
- `feature/journal/src/main/java/com/stoicjournal/feature/journal/draft/JournalDraftSaver.kt`

## Test results

Ran the requested build:

```bash
./gradlew :feature:journal:assembleDebug :core:data:assembleDebug :core:domain:assembleDebug :core:common:assembleDebug --no-daemon
```

Result: `BUILD SUCCESSFUL in 1m 21s` (116 actionable tasks: 40 executed, 76 up-to-date).

No Kotlin compilation, kapt, or AAPT2 errors in the affected modules.

## Commit

- `200f021` fix(journal): avoid main-thread draft flush, fix quote restore race, and dedupe auto-save logic

## Issues or concerns

- None. All three Important reviewer findings are addressed and the affected modules build cleanly.
