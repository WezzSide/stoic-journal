## Task 3: Set up Room database, entities, DAOs, and seed bundled content

**Files:**
- Create: `core/data/src/main/java/com/stoicjournal/core/data/entity/JournalEntryEntity.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/entity/QuoteEntity.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/entity/PromptEntity.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/entity/ExerciseEntity.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/entity/PhilosopherEntity.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/dao/JournalEntryDao.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/dao/QuoteDao.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/dao/PromptDao.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/dao/ExerciseDao.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/dao/PhilosopherDao.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/database/StoicJournalDatabase.kt`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/seed/AssetSeedImporter.kt`
- Create: `core/data/src/main/assets/quotes.json`
- Create: `core/data/src/main/assets/prompts.json`
- Create: `core/data/src/main/assets/exercises.json`
- Create: `core/data/src/main/assets/philosophers.json`
- Create: `core/data/src/main/java/com/stoicjournal/core/data/di/DataModule.kt`
- Create: `core/data/build.gradle.kts`

**Interfaces:**
- Consumes: Domain models from `core:domain`.
- Produces: Room database, DAOs, seed importer, and Hilt module providing repositories and database.

- [ ] **Step 1: Write `core/data/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.library")
    id("stoic.android.hilt")
    id("stoic.android.room")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.stoicjournal.core.data"
    buildFeatures {
        compose = false
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    kapt(libs.androidx.room.compiler)
}
```

- [ ] **Step 2: Write `JournalEntryEntity.kt`**

```kotlin
package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stoicjournal.core.domain.model.EntryType

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val type: EntryType,
    val promptId: String? = null,
    val title: String? = null,
    val body: String,
    val quoteId: String? = null,
    val exerciseId: String? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
```

- [ ] **Step 3: Write `QuoteEntity.kt`**

```kotlin
package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stoicjournal.core.domain.model.Theme

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val id: String,
    val text: String,
    val attribution: String,
    val philosopherId: String,
    val theme: Theme,
    val source: String? = null,
    val isFavorite: Boolean = false
)
```

- [ ] **Step 4: Write `PromptEntity.kt`**

```kotlin
package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.model.Theme

@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey val id: String,
    val text: String,
    val theme: Theme,
    val philosopherId: String? = null,
    val difficultyLevel: StoicismLevel = StoicismLevel.BEGINNER
)
```

- [ ] **Step 5: Write `ExerciseEntity.kt`**

```kotlin
package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.model.Theme

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val steps: String, // JSON array string
    val theme: Theme,
    val difficultyLevel: StoicismLevel = StoicismLevel.BEGINNER
)
```

- [ ] **Step 6: Write `PhilosopherEntity.kt`**

```kotlin
package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "philosophers")
data class PhilosopherEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lived: String,
    val shortBio: String
)
```

- [ ] **Step 7: Write `JournalEntryDao.kt`**

```kotlin
package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stoicjournal.core.data.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries ORDER BY createdAtMillis DESC")
    fun getAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: String): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntryEntity)

    @Delete
    suspend fun delete(entry: JournalEntryEntity)
}
```

- [ ] **Step 8: Write `QuoteDao.kt`**

```kotlin
package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.stoicjournal.core.data.entity.QuoteEntity
import com.stoicjournal.core.domain.model.Theme
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes ORDER BY id ASC")
    fun getAll(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE philosopherId = :philosopherId ORDER BY id ASC")
    fun getByPhilosopher(philosopherId: String): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE theme = :theme ORDER BY id ASC")
    fun getByTheme(theme: Theme): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE isFavorite = 1 ORDER BY id ASC")
    fun getFavorites(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE id = :id")
    suspend fun getById(id: String): QuoteEntity?

    @Update
    suspend fun update(quote: QuoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotes: List<QuoteEntity>)
}
```

- [ ] **Step 9: Write `PromptDao.kt`**

```kotlin
package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stoicjournal.core.data.entity.PromptEntity
import com.stoicjournal.core.domain.model.StoicismLevel
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts ORDER BY id ASC")
    fun getAll(): Flow<List<PromptEntity>>

    @Query("SELECT * FROM prompts WHERE difficultyLevel = :level ORDER BY id ASC")
    fun getByDifficulty(level: StoicismLevel): Flow<List<PromptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prompts: List<PromptEntity>)
}
```

- [ ] **Step 10: Write `ExerciseDao.kt`**

```kotlin
package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stoicjournal.core.data.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY id ASC")
    fun getAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)
}
```

- [ ] **Step 11: Write `PhilosopherDao.kt`**

```kotlin
package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stoicjournal.core.data.entity.PhilosopherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhilosopherDao {
    @Query("SELECT * FROM philosophers ORDER BY name ASC")
    fun getAll(): Flow<List<PhilosopherEntity>>

    @Query("SELECT * FROM philosophers WHERE id = :id")
    suspend fun getById(id: String): PhilosopherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(philosophers: List<PhilosopherEntity>)
}
```

- [ ] **Step 12: Write `StoicJournalDatabase.kt`**

```kotlin
package com.stoicjournal.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.stoicjournal.core.data.dao.ExerciseDao
import com.stoicjournal.core.data.dao.JournalEntryDao
import com.stoicjournal.core.data.dao.PhilosopherDao
import com.stoicjournal.core.data.dao.PromptDao
import com.stoicjournal.core.data.dao.QuoteDao
import com.stoicjournal.core.data.entity.ExerciseEntity
import com.stoicjournal.core.data.entity.JournalEntryEntity
import com.stoicjournal.core.data.entity.PhilosopherEntity
import com.stoicjournal.core.data.entity.PromptEntity
import com.stoicjournal.core.data.entity.QuoteEntity
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.model.Theme

@Database(
    entities = [
        JournalEntryEntity::class,
        QuoteEntity::class,
        PromptEntity::class,
        ExerciseEntity::class,
        PhilosopherEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(StoicJournalTypeConverters::class)
abstract class StoicJournalDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun quoteDao(): QuoteDao
    abstract fun promptDao(): PromptDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun philosopherDao(): PhilosopherDao
}

class StoicJournalTypeConverters {
    @TypeConverter
    fun fromEntryType(value: EntryType): String = value.name

    @TypeConverter
    fun toEntryType(value: String): EntryType = EntryType.valueOf(value)

    @TypeConverter
    fun fromStoicismLevel(value: StoicismLevel): String = value.name

    @TypeConverter
    fun toStoicismLevel(value: String): StoicismLevel = StoicismLevel.valueOf(value)

    @TypeConverter
    fun fromTheme(value: Theme): String = value.name

    @TypeConverter
    fun toTheme(value: String): Theme = Theme.valueOf(value)
}
```

- [ ] **Step 13: Write `AssetSeedImporter.kt`**

```kotlin
package com.stoicjournal.core.data.seed

import android.content.Context
import com.stoicjournal.core.data.dao.ExerciseDao
import com.stoicjournal.core.data.dao.PhilosopherDao
import com.stoicjournal.core.data.dao.PromptDao
import com.stoicjournal.core.data.dao.QuoteDao
import com.stoicjournal.core.data.entity.ExerciseEntity
import com.stoicjournal.core.data.entity.PhilosopherEntity
import com.stoicjournal.core.data.entity.PromptEntity
import com.stoicjournal.core.data.entity.QuoteEntity
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.model.Theme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import javax.inject.Inject

class AssetSeedImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val quoteDao: QuoteDao,
    private val promptDao: PromptDao,
    private val exerciseDao: ExerciseDao,
    private val philosopherDao: PhilosopherDao
) {
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (quoteDao.getAll().first().isNotEmpty()) return@withContext
        val json = Json { ignoreUnknownKeys = true }

        context.assets.open("quotes.json").use { stream ->
            val dtos: List<QuoteDto> = json.decodeFromStream(stream)
            quoteDao.insertAll(dtos.map { it.toEntity() })
        }
        context.assets.open("prompts.json").use { stream ->
            val dtos: List<PromptDto> = json.decodeFromStream(stream)
            promptDao.insertAll(dtos.map { it.toEntity() })
        }
        context.assets.open("exercises.json").use { stream ->
            val dtos: List<ExerciseDto> = json.decodeFromStream(stream)
            exerciseDao.insertAll(dtos.map { it.toEntity() })
        }
        context.assets.open("philosophers.json").use { stream ->
            val dtos: List<PhilosopherDto> = json.decodeFromStream(stream)
            philosopherDao.insertAll(dtos.map { it.toEntity() })
        }
    }
}

@Serializable
private data class QuoteDto(
    val id: String,
    val text: String,
    val attribution: String,
    val philosopherId: String,
    val theme: String,
    val source: String? = null
) {
    fun toEntity() = QuoteEntity(
        id = id,
        text = text,
        attribution = attribution,
        philosopherId = philosopherId,
        theme = Theme.valueOf(theme),
        source = source
    )
}

@Serializable
private data class PromptDto(
    val id: String,
    val text: String,
    val theme: String,
    val philosopherId: String? = null,
    val difficultyLevel: String = "BEGINNER"
) {
    fun toEntity() = PromptEntity(
        id = id,
        text = text,
        theme = Theme.valueOf(theme),
        philosopherId = philosopherId,
        difficultyLevel = StoicismLevel.valueOf(difficultyLevel)
    )
}

@Serializable
private data class ExerciseDto(
    val id: String,
    val title: String,
    val description: String,
    val steps: List<String>,
    val theme: String,
    val difficultyLevel: String = "BEGINNER"
) {
    fun toEntity() = ExerciseEntity(
        id = id,
        title = title,
        description = description,
        steps = steps.joinToString("|"),
        theme = Theme.valueOf(theme),
        difficultyLevel = StoicismLevel.valueOf(difficultyLevel)
    )
}

@Serializable
private data class PhilosopherDto(
    val id: String,
    val name: String,
    val lived: String,
    val shortBio: String
) {
    fun toEntity() = PhilosopherEntity(
        id = id,
        name = name,
        lived = lived,
        shortBio = shortBio
    )
}
```

- [ ] **Step 14: Write bundled `quotes.json`** (sample subset)

```json
[
  {
    "id": "marcus-001",
    "text": "You have power over your mind — not outside events. Realize this, and you will find strength.",
    "attribution": "Marcus Aurelius",
    "philosopherId": "marcus-aurelius",
    "theme": "CONTROL",
    "source": "Meditations"
  },
  {
    "id": "epictetus-001",
    "text": "It's not what happens to you, but how you react to it that matters.",
    "attribution": "Epictetus",
    "philosopherId": "epictetus",
    "theme": "ADVERSITY"
  },
  {
    "id": "seneca-001",
    "text": "We suffer more often in imagination than in reality.",
    "attribution": "Seneca",
    "philosopherId": "seneca",
    "theme": "ADVERSITY"
  }
]
```

- [ ] **Step 15: Write bundled `prompts.json`** (sample subset)

```json
[
  {
    "id": "prompt-control-001",
    "text": "What is within your control today, and what is not? How will you focus your energy on the former?",
    "theme": "CONTROL",
    "difficultyLevel": "BEGINNER"
  },
  {
    "id": "prompt-adversity-001",
    "text": "Recall a recent difficulty. What virtue can you practice in response to it?",
    "theme": "ADVERSITY",
    "difficultyLevel": "FAMILIAR"
  },
  {
    "id": "prompt-mortality-001",
    "text": "If you knew this were your last day, what would you stop doing and what would you begin?",
    "theme": "MORTALITY",
    "difficultyLevel": "ADVANCED"
  }
]
```

- [ ] **Step 16: Write bundled `exercises.json`**

```json
[
  {
    "id": "exercise-dichotomy-of-control",
    "title": "Dichotomy of Control",
    "description": "Separate what is in your control from what is not.",
    "steps": [
      "Take three slow breaths.",
      "List one thing that is troubling you.",
      "Ask: Is this entirely within my control, partly within my control, or outside my control?",
      "For what is within your control, name one virtuous action.",
      "For what is outside your control, practice accepting it as it is."
    ],
    "theme": "CONTROL",
    "difficultyLevel": "BEGINNER"
  },
  {
    "id": "exercise-evening-review",
    "title": "Evening Review",
    "description": "Reflect on the day’s actions and judgments.",
    "steps": [
      "Where did I act well today?",
      "Where did I fall short of my values?",
      "What could I do differently tomorrow?",
      "Write one sentence of self-forgiveness."
    ],
    "theme": "WISDOM",
    "difficultyLevel": "FAMILIAR"
  },
  {
    "id": "exercise-premeditatio-malorum",
    "title": "Premeditatio Malorum",
    "description": "Anticipate challenges with calm.",
    "steps": [
      "Identify one challenge you may face tomorrow.",
      "Imagine it happening in detail.",
      "Ask: What resources do I have to meet it?",
      "Commit to one virtuous response."
    ],
    "theme": "ADVERSITY",
    "difficultyLevel": "FAMILIAR"
  },
  {
    "id": "exercise-view-from-above",
    "title": "View from Above",
    "description": "Gain perspective by imagining your place in the wider world.",
    "steps": [
      "Close your eyes and picture your current location from above.",
      "Zoom out to see your neighborhood, city, country, planet.",
      "Notice how small today's concern appears.",
      "Return with one calm intention."
    ],
    "theme": "MORTALITY",
    "difficultyLevel": "ADVANCED"
  },
  {
    "id": "exercise-amor-fati",
    "title": "Amor Fati",
    "description": "Reflect on accepting what happens.",
    "steps": [
      "Write down one thing you wish were different.",
      "Ask: What could this obstacle teach me?",
      "Write one reason to be grateful it happened exactly this way.",
      "Choose to meet tomorrow with open hands."
    ],
    "theme": "COURAGE",
    "difficultyLevel": "ADVANCED"
  }
]
```

- [ ] **Step 17: Write bundled `philosophers.json`**

```json
[
  {
    "id": "marcus-aurelius",
    "name": "Marcus Aurelius",
    "lived": "121–180 AD",
    "shortBio": "Roman emperor and Stoic philosopher, author of the Meditations."
  },
  {
    "id": "epictetus",
    "name": "Epictetus",
    "lived": "c. 50–135 AD",
    "shortBio": "A formerly enslaved Stoic teacher whose Discourses emphasize the dichotomy of control."
  },
  {
    "id": "seneca",
    "name": "Seneca the Younger",
    "lived": "c. 4 BC–65 AD",
    "shortBio": "Roman Stoic philosopher, statesman, and playwright."
  },
  {
    "id": "musonius-rufus",
    "name": "Musonius Rufus",
    "lived": "c. 30–100 AD",
    "shortBio": "Roman Stoic teacher whose lectures influenced Epictetus."
  },
  {
    "id": "cleanthes",
    "name": "Cleanthes",
    "lived": "c. 330–230 BC",
    "shortBio": "Second head of the Stoic school, known for his Hymn to Zeus."
  },
  {
    "id": "cato-the-younger",
    "name": "Cato the Younger",
    "lived": "95–46 BC",
    "shortBio": "Roman senator celebrated in Stoic tradition as a model of integrity."
  }
]
```

- [ ] **Step 18: Write `DataModule.kt`**

```kotlin
package com.stoicjournal.core.data.di

import android.content.Context
import androidx.room.Room
import com.stoicjournal.core.data.database.StoicJournalDatabase
import com.stoicjournal.core.data.repository.ExerciseRepository
import com.stoicjournal.core.data.repository.ExerciseRepositoryImpl
import com.stoicjournal.core.data.repository.JournalRepository
import com.stoicjournal.core.data.repository.JournalRepositoryImpl
import com.stoicjournal.core.data.repository.PhilosopherRepository
import com.stoicjournal.core.data.repository.PhilosopherRepositoryImpl
import com.stoicjournal.core.data.repository.PreferencesRepository
import com.stoicjournal.core.data.repository.PreferencesRepositoryImpl
import com.stoicjournal.core.data.repository.PromptRepository
import com.stoicjournal.core.data.repository.PromptRepositoryImpl
import com.stoicjournal.core.data.repository.QuoteRepository
import com.stoicjournal.core.data.repository.QuoteRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindJournalRepository(impl: JournalRepositoryImpl): JournalRepository

    @Binds
    abstract fun bindQuoteRepository(impl: QuoteRepositoryImpl): QuoteRepository

    @Binds
    abstract fun bindPromptRepository(impl: PromptRepositoryImpl): PromptRepository

    @Binds
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    abstract fun bindPhilosopherRepository(impl: PhilosopherRepositoryImpl): PhilosopherRepository

    @Binds
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StoicJournalDatabase {
        return Room.databaseBuilder(
            context,
            StoicJournalDatabase::class.java,
            "stoic_journal.db"
        ).build()
    }

    @Provides
    fun provideJournalEntryDao(db: StoicJournalDatabase) = db.journalEntryDao()

    @Provides
    fun provideQuoteDao(db: StoicJournalDatabase) = db.quoteDao()

    @Provides
    fun providePromptDao(db: StoicJournalDatabase) = db.promptDao()

    @Provides
    fun provideExerciseDao(db: StoicJournalDatabase) = db.exerciseDao()

    @Provides
    fun providePhilosopherDao(db: StoicJournalDatabase) = db.philosopherDao()
}
```

- [ ] **Step 19: Add kotlinx serialization to version catalog**

Modify `gradle/libs.versions.toml`:

```toml
[versions]
kotlinxSerialization = "1.6.3"

[libraries]
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 20: Build the data module**

Run: `./gradlew :core:data:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 21: Commit**

```bash
git add core/data/ gradle/libs.versions.toml
git commit -m "feat(data): add Room database, entities, DAOs, and bundled content seeding"
```

---

