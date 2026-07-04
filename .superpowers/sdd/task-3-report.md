# Task 3 Report: Set up Room database, entities, DAOs, and seed bundled content

## What I implemented

Created the `:core:data` Android library module with:

- **Build config**
  - `core/data/build.gradle.kts` — applies `stoic.android.library`, `stoic.android.hilt`, `stoic.android.room`, and Kotlinx Serialization plugins; depends on `:core:domain`, `:core:common`, Room, DataStore, and Kotlinx Serialization.
  - `gradle/libs.versions.toml` — added `kotlinxSerialization = "1.6.3"`, the `kotlinx-serialization-json` library, and the `kotlin-serialization` plugin.
  - `core/common/build.gradle.kts` — minimal `:core:common` library module so the `:core:data` dependency resolves.

- **Room layer**
  - Five entities: `JournalEntryEntity`, `QuoteEntity`, `PromptEntity`, `ExerciseEntity`, `PhilosopherEntity`.
  - Five DAOs: `JournalEntryDao`, `QuoteDao`, `PromptDao`, `ExerciseDao`, `PhilosopherDao`.
  - `StoicJournalDatabase` with `StoicJournalTypeConverters` for `EntryType`, `StoicismLevel`, and `Theme` enums.

- **Seed importer**
  - `AssetSeedImporter` reads bundled JSON assets and inserts them only when the quote table is empty.

- **Bundled JSON assets**
  - `quotes.json`, `prompts.json`, `exercises.json`, `philosophers.json` with the sample content from the task brief.

- **Dependency injection**
  - `DataModule.kt` providing the Room database, all DAOs, and repository bindings.
  - Repository interfaces and implementations: `JournalRepository`, `QuoteRepository`, `PromptRepository`, `ExerciseRepository`, `PhilosopherRepository`, `PreferencesRepository` and their `*Impl` counterparts. These were required because `DataModule.kt` as specified binds them, but they were not listed in the task file list.

## What I tested and test results

Ran the requested build command:

```bash
./gradlew :core:data:assembleDebug
```

Result: `BUILD SUCCESSFUL in 1m 7s` (52 actionable tasks: 48 executed, 4 up-to-date).

## Files changed

- `gradle/libs.versions.toml`
- `core/common/build.gradle.kts` (new)
- `core/data/build.gradle.kts` (new)
- `core/data/src/main/assets/quotes.json` (new)
- `core/data/src/main/assets/prompts.json` (new)
- `core/data/src/main/assets/exercises.json` (new)
- `core/data/src/main/assets/philosophers.json` (new)
- `core/data/src/main/java/com/stoicjournal/core/data/entity/*.kt` (5 new)
- `core/data/src/main/java/com/stoicjournal/core/data/dao/*.kt` (5 new)
- `core/data/src/main/java/com/stoicjournal/core/data/database/StoicJournalDatabase.kt` (new)
- `core/data/src/main/java/com/stoicjournal/core/data/seed/AssetSeedImporter.kt` (new)
- `core/data/src/main/java/com/stoicjournal/core/data/di/DataModule.kt` (new)
- `core/data/src/main/java/com/stoicjournal/core/data/repository/*.kt` (12 new)

## Self-review findings

1. **Repositories not in task file list.** The specified `DataModule.kt` binds six repository interfaces to their implementations, but the task brief did not list those repository files. I created minimal, injectable repository stubs so the module compiles. They currently expose entity types directly and may need richer behavior/domain-model mapping in a future task.
2. **`:core:common` dependency.** `core/data/build.gradle.kts` depends on `:core:common`, but that module did not exist. I created a minimal `:core:common` library module so the project resolves.
3. **Serialization plugin declaration.** The brief showed `id("org.jetbrains.kotlin.plugin.serialization")`, but plugin resolution failed because pluginManagement could not find an unversioned external plugin. I used `alias(libs.plugins.kotlin.serialization)` instead, which correctly resolves through the version catalog.
4. **No runtime/Robolectric tests.** The task only requested an assemble build; I did not add instrumentation or unit tests for the seed importer or DAOs.

## Issues or concerns

- The repository layer is intentionally minimal. Future tasks will likely need to evolve these into domain-model repositories with use-case-specific operations.
- `AssetSeedImporter` uses `quoteDao.getAll().first().isNotEmpty()` to decide whether to seed. For large quote tables this could be inefficient, but it is fine for the bundled seed data size.
