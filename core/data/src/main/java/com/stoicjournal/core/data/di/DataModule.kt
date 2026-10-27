package com.stoicjournal.core.data.di

import android.content.Context
import androidx.room.Room
import com.stoicjournal.core.data.dao.AiReflectionDao
import com.stoicjournal.core.data.database.StoicJournalDatabase
import com.stoicjournal.core.data.repository.CachedAiRepository
import com.stoicjournal.core.data.repository.ExerciseRepositoryImpl
import com.stoicjournal.core.data.repository.GeminiAiRepositoryImpl
import com.stoicjournal.core.data.repository.JournalDraftRepositoryImpl
import com.stoicjournal.core.data.repository.JournalRepositoryImpl
import com.stoicjournal.core.data.repository.PhilosopherRepositoryImpl
import com.stoicjournal.core.data.repository.PreferencesRepositoryImpl
import com.stoicjournal.core.data.repository.PromptRepositoryImpl
import com.stoicjournal.core.data.repository.QuoteRepositoryImpl
import com.stoicjournal.core.data.repository.SafeAiRepository
import com.stoicjournal.core.domain.repository.AiRepository
import com.stoicjournal.core.domain.repository.ExerciseRepository
import com.stoicjournal.core.domain.repository.JournalDraftRepository
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.repository.PhilosopherRepository
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.PromptRepository
import com.stoicjournal.core.domain.repository.QuoteRepository
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

    @Binds
    abstract fun bindJournalDraftRepository(impl: JournalDraftRepositoryImpl): JournalDraftRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    @Provides
    @Singleton
    fun provideAiRepository(
        geminiRepo: GeminiAiRepositoryImpl,
        aiReflectionDao: AiReflectionDao
    ): AiRepository {
        val cached = CachedAiRepository(geminiRepo, aiReflectionDao)
        return SafeAiRepository(cached)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StoicJournalDatabase {
        val migration1to2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ai_reflections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `content` TEXT NOT NULL, `insight` TEXT NOT NULL, `interpretation` TEXT NOT NULL, `reflectiveQuestion` TEXT NOT NULL, `actionableStep` TEXT NOT NULL, `quote` TEXT, `principle` TEXT, `timestamp` INTEGER NOT NULL, `theme` TEXT NOT NULL)"
                )
            }
        }
        val migration2to3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `journal_entries` ADD COLUMN `virtues` TEXT NOT NULL DEFAULT ''"
                )
            }
        }
        val migration1to3 = object : androidx.room.migration.Migration(1, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ai_reflections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `content` TEXT NOT NULL, `insight` TEXT NOT NULL, `interpretation` TEXT NOT NULL, `reflectiveQuestion` TEXT NOT NULL, `actionableStep` TEXT NOT NULL, `quote` TEXT, `principle` TEXT, `timestamp` INTEGER NOT NULL, `theme` TEXT NOT NULL)"
                )
                db.execSQL(
                    "ALTER TABLE `journal_entries` ADD COLUMN `virtues` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        return Room.databaseBuilder(
            context,
            StoicJournalDatabase::class.java,
            "stoic_journal.db"
        )
            .addMigrations(migration1to2, migration2to3, migration1to3)
            .build()
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

    @Provides
    fun provideAiReflectionDao(db: StoicJournalDatabase) = db.aiReflectionDao()
}
