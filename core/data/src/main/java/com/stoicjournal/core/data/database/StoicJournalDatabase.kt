package com.stoicjournal.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.stoicjournal.core.data.dao.AiReflectionDao
import com.stoicjournal.core.data.dao.ExerciseDao
import com.stoicjournal.core.data.dao.JournalEntryDao
import com.stoicjournal.core.data.dao.PhilosopherDao
import com.stoicjournal.core.data.dao.PromptDao
import com.stoicjournal.core.data.dao.QuoteDao
import com.stoicjournal.core.data.entity.AiReflectionEntity
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
        PhilosopherEntity::class,
        AiReflectionEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(StoicJournalTypeConverters::class)
abstract class StoicJournalDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun quoteDao(): QuoteDao
    abstract fun promptDao(): PromptDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun philosopherDao(): PhilosopherDao
    abstract fun aiReflectionDao(): AiReflectionDao
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
