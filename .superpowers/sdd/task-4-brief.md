## Task 4: Implement repositories and mappers

**Files:**
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/repository/JournalRepository.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/repository/JournalRepositoryImpl.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/repository/QuoteRepository.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/repository/QuoteRepositoryImpl.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/repository/PromptRepository.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/repository/PromptRepositoryImpl.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/repository/ExerciseRepository.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/repository/ExerciseRepositoryImpl.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/repository/PhilosopherRepository.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/repository/PhilosopherRepositoryImpl.kt`
- Create: `core/domain/src/main/java/com/stoicjournal/core/domain/repository/PreferencesRepository.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/repository/PreferencesRepositoryImpl.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/sync/StreakCalculator.kt`
- Create: `core/data/src/test/java/com/stoicjournal/core/data/sync/StreakCalculatorTest.kt`

**Interfaces:**
- Consumes: DAOs, domain models, `AssetSeedImporter`, DataStore.
- Produces: Repository implementations exposing `Flow` and `suspend` methods returning domain models.

- [ ] **Step 1: Write `JournalRepository.kt`**

```kotlin
package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun getAllEntries(): Flow<List<JournalEntry>>
    suspend fun getEntryById(id: String): JournalEntry?
    suspend fun saveEntry(entry: JournalEntry)
    suspend fun deleteEntry(entry: JournalEntry)
}
```

- [ ] **Step 2: Write `JournalRepositoryImpl.kt`**

```kotlin
package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.JournalEntryDao
import com.stoicjournal.core.data.entity.JournalEntryEntity
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class JournalRepositoryImpl @Inject constructor(
    private val journalEntryDao: JournalEntryDao
) : JournalRepository {

    override fun getAllEntries(): Flow<List<JournalEntry>> {
        return journalEntryDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEntryById(id: String): JournalEntry? {
        return journalEntryDao.getById(id)?.toDomain()
    }

    override suspend fun saveEntry(entry: JournalEntry) {
        journalEntryDao.insert(entry.toEntity())
    }

    override suspend fun deleteEntry(entry: JournalEntry) {
        journalEntryDao.delete(entry.toEntity())
    }
}

private fun JournalEntryEntity.toDomain() = JournalEntry(
    id = id,
    type = type,
    promptId = promptId,
    title = title,
    body = body,
    quoteId = quoteId,
    exerciseId = exerciseId,
    createdAt = Instant.ofEpochMilli(createdAtMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtMillis)
)

private fun JournalEntry.toEntity() = JournalEntryEntity(
    id = id,
    type = type,
    promptId = promptId,
    title = title,
    body = body,
    quoteId = quoteId,
    exerciseId = exerciseId,
    createdAtMillis = createdAt.toEpochMilli(),
    updatedAtMillis = updatedAt.toEpochMilli()
)
```

- [ ] **Step 3: Write `QuoteRepository.kt` and `QuoteRepositoryImpl.kt`**

```kotlin
package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Theme
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    fun getAllQuotes(): Flow<List<Quote>>
    fun getQuotesByPhilosopher(philosopherId: String): Flow<List<Quote>>
    fun getQuotesByTheme(theme: Theme): Flow<List<Quote>>
    fun getFavoriteQuotes(): Flow<List<Quote>>
    suspend fun toggleFavorite(quoteId: String)
}
```

```kotlin
package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.QuoteDao
import com.stoicjournal.core.data.entity.QuoteEntity
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Theme
import com.stoicjournal.core.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuoteRepositoryImpl @Inject constructor(
    private val quoteDao: QuoteDao
) : QuoteRepository {

    override fun getAllQuotes(): Flow<List<Quote>> = quoteDao.getAll().map { it.map(QuoteEntity::toDomain) }
    override fun getQuotesByPhilosopher(philosopherId: String): Flow<List<Quote>> =
        quoteDao.getByPhilosopher(philosopherId).map { it.map(QuoteEntity::toDomain) }

    override fun getQuotesByTheme(theme: Theme): Flow<List<Quote>> =
        quoteDao.getByTheme(theme).map { it.map(QuoteEntity::toDomain) }

    override fun getFavoriteQuotes(): Flow<List<Quote>> =
        quoteDao.getFavorites().map { it.map(QuoteEntity::toDomain) }

    override suspend fun toggleFavorite(quoteId: String) {
        val quote = quoteDao.getById(quoteId) ?: return
        quoteDao.update(quote.copy(isFavorite = !quote.isFavorite))
    }
}

private fun QuoteEntity.toDomain() = Quote(
    id = id,
    text = text,
    attribution = attribution,
    philosopherId = philosopherId,
    theme = theme,
    source = source,
    isFavorite = isFavorite
)
```

- [ ] **Step 4: Write `PromptRepository.kt` and `PromptRepositoryImpl.kt`**

```kotlin
package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.StoicismLevel
import kotlinx.coroutines.flow.Flow

interface PromptRepository {
    fun getAllPrompts(): Flow<List<Prompt>>
    fun getPromptsByDifficulty(level: StoicismLevel): Flow<List<Prompt>>
}
```

```kotlin
package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.PromptDao
import com.stoicjournal.core.data.entity.PromptEntity
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.repository.PromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PromptRepositoryImpl @Inject constructor(
    private val promptDao: PromptDao
) : PromptRepository {

    override fun getAllPrompts(): Flow<List<Prompt>> =
        promptDao.getAll().map { it.map(PromptEntity::toDomain) }

    override fun getPromptsByDifficulty(level: StoicismLevel): Flow<List<Prompt>> =
        promptDao.getByDifficulty(level).map { it.map(PromptEntity::toDomain) }
}

private fun PromptEntity.toDomain() = Prompt(
    id = id,
    text = text,
    theme = theme,
    philosopherId = philosopherId,
    difficultyLevel = difficultyLevel
)
```

- [ ] **Step 5: Write `ExerciseRepository.kt` and `ExerciseRepositoryImpl.kt`**

```kotlin
package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    suspend fun getExerciseById(id: String): Exercise?
}
```

```kotlin
package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.ExerciseDao
import com.stoicjournal.core.data.entity.ExerciseEntity
import com.stoicjournal.core.domain.model.Exercise
import com.stoicjournal.core.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> =
        exerciseDao.getAll().map { it.map(ExerciseEntity::toDomain) }

    override suspend fun getExerciseById(id: String): Exercise? =
        exerciseDao.getById(id)?.toDomain()
}

private fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    title = title,
    description = description,
    steps = steps.split("|"),
    theme = theme,
    difficultyLevel = difficultyLevel
)
```

- [ ] **Step 6: Write `PhilosopherRepository.kt` and `PhilosopherRepositoryImpl.kt`**

```kotlin
package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.Philosopher
import kotlinx.coroutines.flow.Flow

interface PhilosopherRepository {
    fun getAllPhilosophers(): Flow<List<Philosopher>>
    suspend fun getPhilosopherById(id: String): Philosopher?
}
```

```kotlin
package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.PhilosopherDao
import com.stoicjournal.core.data.entity.PhilosopherEntity
import com.stoicjournal.core.domain.model.Philosopher
import com.stoicjournal.core.domain.repository.PhilosopherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PhilosopherRepositoryImpl @Inject constructor(
    private val philosopherDao: PhilosopherDao
) : PhilosopherRepository {

    override fun getAllPhilosophers(): Flow<List<Philosopher>> =
        philosopherDao.getAll().map { it.map(PhilosopherEntity::toDomain) }

    override suspend fun getPhilosopherById(id: String): Philosopher? =
        philosopherDao.getById(id)?.toDomain()
}

private fun PhilosopherEntity.toDomain() = Philosopher(
    id = id,
    name = name,
    lived = lived,
    shortBio = shortBio
)
```

- [ ] **Step 7: Write `PreferencesRepository.kt` and `PreferencesRepositoryImpl.kt`**

```kotlin
package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.StoicismLevel
import kotlinx.coroutines.flow.Flow

data class UserPreferences(
    val reminderHour: Int,
    val reminderMinute: Int,
    val reminderEnabled: Boolean,
    val stoicismLevel: StoicismLevel,
    val onboardingComplete: Boolean
)

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setReminderTime(hour: Int, minute: Int)
    suspend fun setReminderEnabled(enabled: Boolean)
    suspend fun setStoicismLevel(level: StoicismLevel)
    suspend fun setOnboardingComplete(complete: Boolean)
}
```

```kotlin
package com.stoicjournal.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stoic_journal_preferences")

class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private val dataStore = context.dataStore

    companion object {
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val STOICISM_LEVEL = intPreferencesKey("stoicism_level")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            reminderHour = prefs[REMINDER_HOUR] ?: 20,
            reminderMinute = prefs[REMINDER_MINUTE] ?: 0,
            reminderEnabled = prefs[REMINDER_ENABLED] ?: true,
            stoicismLevel = StoicismLevel.entries[prefs[STOICISM_LEVEL] ?: 0],
            onboardingComplete = prefs[ONBOARDING_COMPLETE] ?: false
        )
    }

    override suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[REMINDER_HOUR] = hour
            prefs[REMINDER_MINUTE] = minute
        }
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[REMINDER_ENABLED] = enabled
        }
    }

    override suspend fun setStoicismLevel(level: StoicismLevel) {
        dataStore.edit { prefs ->
            prefs[STOICISM_LEVEL] = level.ordinal
        }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE] = complete
        }
    }
}
```

- [ ] **Step 8: Write `StreakCalculator.kt`**

```kotlin
package com.stoicjournal.core.data.sync

import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.model.Streak
import java.time.LocalDate
import java.time.ZoneId

object StreakCalculator {

    fun calculate(entries: List<JournalEntry>): Streak {
        if (entries.isEmpty()) return Streak(currentCount = 0, lastEntryDate = null)
        val dates = entries
            .map { it.createdAt.atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sortedDescending()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val mostRecent = dates.first()
        if (mostRecent != today && mostRecent != yesterday) {
            return Streak(currentCount = 0, lastEntryDate = mostRecent)
        }
        var streak = 1
        var current = mostRecent
        for (date in dates.drop(1)) {
            if (date == current.minusDays(1)) {
                streak++
                current = date
            } else {
                break
            }
        }
        return Streak(currentCount = streak, lastEntryDate = mostRecent)
    }
}
```

- [ ] **Step 9: Write `StreakCalculatorTest.kt`**

```kotlin
package com.stoicjournal.core.data.sync

import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StreakCalculatorTest {

    @Test
    fun `empty entries returns zero streak`() {
        val result = StreakCalculator.calculate(emptyList())
        assertEquals(0, result.currentCount)
        assertEquals(null, result.lastEntryDate)
    }

    @Test
    fun `consecutive days returns correct streak`() {
        val entries = (0L..2L).map { daysAgo ->
            entryOn(LocalDate.now().minusDays(daysAgo))
        }
        val result = StreakCalculator.calculate(entries)
        assertEquals(3, result.currentCount)
    }

    @Test
    fun `gap breaks streak`() {
        val entries = listOf(
            entryOn(LocalDate.now()),
            entryOn(LocalDate.now().minusDays(2))
        )
        val result = StreakCalculator.calculate(entries)
        assertEquals(1, result.currentCount)
    }

    private fun entryOn(date: LocalDate): JournalEntry {
        val instant = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        return JournalEntry(
            id = instant.toString(),
            type = EntryType.GUIDED,
            body = "",
            createdAt = instant,
            updatedAt = instant
        )
    }
}
```

- [ ] **Step 10: Run repository tests**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, tests pass.

- [ ] **Step 11: Commit**

```bash
git add core/data/
git commit -m "feat(data): add repositories, preferences, and streak calculator"
```

---

