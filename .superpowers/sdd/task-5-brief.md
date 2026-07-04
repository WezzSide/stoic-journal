## Task 5: Implement use cases in core:domain

**Files:**
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/GetTodayPromptUseCase.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/SaveJournalEntryUseCase.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/GetJournalEntriesUseCase.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/ToggleFavoriteQuoteUseCase.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/GetStreakUseCase.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/usecase/GetQuoteForPromptUseCase.kt`
- Modify: `core/domain/build.gradle.kts`

**Interfaces:**
- Consumes: Repositories from `core:data`.
- Produces: Use cases injected into ViewModels.

- [ ] **Step 1: Add Hilt dependency to `core/domain/build.gradle.kts`**

Repository interfaces live in `core:domain`, so use cases here depend only on interfaces, not on `core:data`.

```kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

- [ ] **Step 2: Ensure repository interfaces are in `core:domain`**

Create `core/domain/src/main/java/com/stoicjournal/core/domain/repository/` and move all `*Repository.kt` interface files there.

Update package declarations from `com.stoicjournal.core.data.repository` to `com.stoicjournal.core.domain.repository`.

- [ ] **Step 3: Update `DataModule.kt` to bind interfaces from domain package**

Change imports:

```kotlin
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.repository.QuoteRepository
import com.stoicjournal.core.domain.repository.PromptRepository
import com.stoicjournal.core.domain.repository.ExerciseRepository
import com.stoicjournal.core.domain.repository.PhilosopherRepository
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.UserPreferences
```

- [ ] **Step 4: Write `GetTodayPromptUseCase.kt`**

```kotlin
package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.PromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

class GetTodayPromptUseCase @Inject constructor(
    private val promptRepository: PromptRepository,
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<TodayPrompt> = combine(
        promptRepository.getAllPrompts(),
        preferencesRepository.preferences
    ) { prompts, prefs ->
        val candidates = if (prefs.stoicismLevel == StoicismLevel.BEGINNER) {
            prompts.filter { it.difficultyLevel == StoicismLevel.BEGINNER }
        } else {
            prompts
        }
        val selected = candidates.ifEmpty { prompts }.let {
            it[LocalDate.now().dayOfYear % it.size]
        }
        TodayPrompt(
            prompt = selected,
            date = LocalDate.now()
        )
    }
}

data class TodayPrompt(
    val prompt: Prompt,
    val date: LocalDate
)
```

- [ ] **Step 5: Write `GetQuoteForPromptUseCase.kt`**

```kotlin
package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetQuoteForPromptUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository
) {
    operator fun invoke(prompt: Prompt): Flow<Quote?> =
        quoteRepository.getAllQuotes().map { quotes ->
            val matching = quotes.filter { it.theme == prompt.theme || it.philosopherId == prompt.philosopherId }
            matching.firstOrNull() ?: quotes.firstOrNull()
        }
}
```

- [ ] **Step 6: Write `SaveJournalEntryUseCase.kt`**

```kotlin
package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class SaveJournalEntryUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    suspend operator fun invoke(entry: JournalEntry) {
        journalRepository.saveEntry(entry)
    }
}

fun createJournalEntry(
    type: com.stoicjournal.core.domain.model.EntryType,
    body: String,
    promptId: String? = null,
    title: String? = null,
    quoteId: String? = null,
    exerciseId: String? = null
): JournalEntry {
    val now = Instant.now()
    return JournalEntry(
        id = UUID.randomUUID().toString(),
        type = type,
        promptId = promptId,
        title = title,
        body = body,
        quoteId = quoteId,
        exerciseId = exerciseId,
        createdAt = now,
        updatedAt = now
    )
}
```

- [ ] **Step 7: Write `GetJournalEntriesUseCase.kt`**

```kotlin
package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetJournalEntriesUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    operator fun invoke(filter: EntryType? = null): Flow<List<JournalEntry>> {
        return journalRepository.getAllEntries().map { entries ->
            filter?.let { entries.filter { entry -> entry.type == filter } } ?: entries
        }
    }
}
```

- [ ] **Step 8: Write `ToggleFavoriteQuoteUseCase.kt`**

```kotlin
package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.repository.QuoteRepository
import javax.inject.Inject

class ToggleFavoriteQuoteUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository
) {
    suspend operator fun invoke(quoteId: String) {
        quoteRepository.toggleFavorite(quoteId)
    }
}
```

- [ ] **Step 9: Write `GetStreakUseCase.kt`**

```kotlin
package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.data.sync.StreakCalculator
import com.stoicjournal.core.domain.model.Streak
import com.stoicjournal.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetStreakUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    operator fun invoke(): Flow<Streak> =
        journalRepository.getAllEntries().map { StreakCalculator.calculate(it) }
}
```

- [ ] **Step 10: Build core modules**

Run: `./gradlew :core:domain:assembleDebug :core:data:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 11: Commit**

```bash
git add core/domain/ core/data/
git commit -m "feat(domain): add use cases and move repository interfaces to domain"
```

---

