# Task 4 Report: Implement repositories and mappers

## What was implemented

1. Moved repository interfaces from `core:data` to `core:domain`:
   - `JournalRepository`
   - `QuoteRepository`
   - `PromptRepository`
   - `ExerciseRepository`
   - `PhilosopherRepository`
   - `PreferencesRepository` (including `UserPreferences` data class)

2. Rewrote repository implementations in `core:data` to:
   - Implement the new domain interfaces
   - Expose domain models instead of Room entities
   - Add private entity-to-domain mapper extensions for each repository
   - Update method names to match the domain contracts (e.g. `getAllEntries`, `getEntryById`, `saveEntry`, `deleteEntry`, `getAllQuotes`, `getQuotesByPhilosopher`, `toggleFavorite`, etc.)

3. Added `StreakCalculator` in `core/data/src/main/java/com/stoicjournal/core/data/sync/StreakCalculator.kt` with logic to calculate current journaling streak from a list of `JournalEntry` domain models.

4. Added `StreakCalculatorTest` in `core/data/src/test/java/com/stoicjournal/core/data/sync/StreakCalculatorTest.kt` covering:
   - Empty entries return zero streak
   - Consecutive days return correct streak
   - A gap breaks the streak

5. Updated `DataModule.kt` to bind implementations against the domain repository interfaces.

6. Added `testImplementation(libs.junit)` to `core:data/build.gradle.kts` so unit tests compile and run.

7. Removed the old entity-exposing repository interface files from `core:data`.

## What was tested

Ran the required unit test command:

```bash
./gradlew :core:data:testDebugUnitTest
```

Result: `BUILD SUCCESSFUL` in ~45s.

Test report (`core/data/build/test-results/testDebugUnitTest/TEST-com.stoicjournal.core.data.sync.StreakCalculatorTest.xml`):
- 3 tests
- 0 skipped
- 0 failures
- 0 errors

## Files changed

### Created
- `core/domain/src/main/java/com/stoicjournal/core/domain/repository/JournalRepository.kt`
- `core/domain/src/main/java/com/stoicjournal/core/domain/repository/QuoteRepository.kt`
- `core/domain/src/main/java/com/stoicjournal/core/domain/repository/PromptRepository.kt`
- `core/domain/src/main/java/com/stoicjournal/core/domain/repository/ExerciseRepository.kt`
- `core/domain/src/main/java/com/stoicjournal/core/domain/repository/PhilosopherRepository.kt`
- `core/domain/src/main/java/com/stoicjournal/core/domain/repository/PreferencesRepository.kt`
- `core/data/src/main/java/com/stoicjournal/core/data/sync/StreakCalculator.kt`
- `core/data/src/test/java/com/stoicjournal/core/data/sync/StreakCalculatorTest.kt`
- `.superpowers/sdd/task-4-report.md`

### Modified
- `core/data/src/main/java/com/stoicjournal/core/data/repository/JournalRepositoryImpl.kt`
- `core/data/src/main/java/com/stoicjournal/core/data/repository/QuoteRepositoryImpl.kt`
- `core/data/src/main/java/com/stoicjournal/core/data/repository/PromptRepositoryImpl.kt`
- `core/data/src/main/java/com/stoicjournal/core/data/repository/ExerciseRepositoryImpl.kt`
- `core/data/src/main/java/com/stoicjournal/core/data/repository/PhilosopherRepositoryImpl.kt`
- `core/data/src/main/java/com/stoicjournal/core/data/repository/PreferencesRepositoryImpl.kt`
- `core/data/src/main/java/com/stoicjournal/core/data/di/DataModule.kt`
- `core/data/build.gradle.kts`

### Deleted
- `core/data/src/main/java/com/stoicjournal/core/data/repository/JournalRepository.kt`
- `core/data/src/main/java/com/stoicjournal/core/data/repository/QuoteRepository.kt`
- `core/data/src/main/java/com/stoicjournal/core/data/repository/PromptRepository.kt`
- `core/data/src/main/java/com/stoicjournal/core/data/repository/ExerciseRepository.kt`
- `core/data/src/main/java/com/stoicjournal/core/data/repository/PhilosopherRepository.kt`
- `core/data/src/main/java/com/stoicjournal/core/data/repository/PreferencesRepository.kt`

## Self-review findings

- All repository implementations now correctly return domain models via private mapper functions.
- `DataModule` binds interfaces from `com.stoicjournal.core.domain.repository`.
- `StreakCalculator` handles empty input, consecutive days, gaps, and yesterday-as-most-recent correctly.
- `PreferencesRepositoryImpl` stores `StoicismLevel` by ordinal and reads it back with `StoicismLevel.entries[index]`; valid indices are guaranteed by the enum definition.
- Build and tests pass.

## Issues or concerns

None.
