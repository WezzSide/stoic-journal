package com.stoicjournal.core.data.di

import android.content.Context
import androidx.room.Room
import com.stoicjournal.core.data.dao.AiReflectionDao
import com.stoicjournal.core.data.dao.MentorConversationDao
import com.stoicjournal.core.data.dao.MentorMessageDao
import com.stoicjournal.core.data.database.StoicJournalDatabase
import com.stoicjournal.core.data.database.StoicJournalMigrations
import com.stoicjournal.core.data.repository.CachedAiRepository
import com.stoicjournal.core.data.repository.DailyReflectionStatusRepositoryImpl
import com.stoicjournal.core.data.repository.AiProviderSelectionRepositoryImpl
import com.stoicjournal.core.data.repository.DefaultAiProviderRegistry
import com.stoicjournal.core.data.repository.ExerciseRepositoryImpl
import com.stoicjournal.core.data.repository.GeminiAiRepositoryImpl
import com.stoicjournal.core.data.repository.JournalDraftRepositoryImpl
import com.stoicjournal.core.data.repository.JournalRepositoryImpl
import com.stoicjournal.core.data.repository.MentorConversationRepositoryImpl
import com.stoicjournal.core.data.repository.MentorReflectionLinkRepositoryImpl
import com.stoicjournal.core.data.dao.MentorMemoryDao
import com.stoicjournal.core.data.repository.MentorMemoryPreferencesRepositoryImpl
import com.stoicjournal.core.data.repository.MentorMemoryRepositoryImpl
import com.stoicjournal.core.data.repository.PhilosopherRepositoryImpl
import com.stoicjournal.core.data.repository.ProactiveMentorPreferencesRepositoryImpl
import com.stoicjournal.core.data.repository.ReflectionCandidateCacheImpl
import com.stoicjournal.core.data.repository.ReflectionPersistenceCoordinatorImpl
import com.stoicjournal.core.domain.repository.MentorMemoryPreferencesRepository
import com.stoicjournal.core.domain.repository.ProactiveMentorPreferencesRepository
import com.stoicjournal.core.data.repository.PreferencesRepositoryImpl
import com.stoicjournal.core.data.repository.PromptRepositoryImpl
import com.stoicjournal.core.data.repository.QuoteRepositoryImpl
import com.stoicjournal.core.data.repository.SafeAiRepository
import com.stoicjournal.core.domain.model.AiProviderRegistry
import com.stoicjournal.core.domain.repository.AiRepository
import com.stoicjournal.core.domain.repository.DailyReflectionStatusRepository
import com.stoicjournal.core.domain.repository.ExerciseRepository
import com.stoicjournal.core.domain.repository.JournalDraftRepository
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.repository.PhilosopherRepository
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.PromptRepository
import com.stoicjournal.core.domain.repository.QuoteRepository
import com.stoicjournal.core.domain.repository.StoicMentorRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

import com.stoicjournal.core.domain.repository.ReflectionCandidateCache

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

    @Binds
    abstract fun bindAiProviderRegistry(impl: DefaultAiProviderRegistry): AiProviderRegistry

    @Binds
    abstract fun bindAiProviderSelectionRepository(impl: AiProviderSelectionRepositoryImpl): com.stoicjournal.core.domain.repository.AiProviderSelectionRepository

    @Binds
    abstract fun bindStoicMentorRepository(impl: MentorConversationRepositoryImpl): StoicMentorRepository

    @Binds
    abstract fun bindDailyReflectionStatusRepository(impl: DailyReflectionStatusRepositoryImpl): DailyReflectionStatusRepository

    @Binds
    abstract fun bindMentorReflectionLinkRepository(impl: MentorReflectionLinkRepositoryImpl): com.stoicjournal.core.domain.repository.MentorReflectionLinkRepository

    @Binds
    abstract fun bindProactiveMentorPreferencesRepository(impl: ProactiveMentorPreferencesRepositoryImpl): ProactiveMentorPreferencesRepository

    @Binds
    abstract fun bindMentorMemoryPreferencesRepository(impl: MentorMemoryPreferencesRepositoryImpl): MentorMemoryPreferencesRepository

    @Binds
    abstract fun bindMentorMemoryRepository(impl: MentorMemoryRepositoryImpl): com.stoicjournal.core.domain.repository.MentorMemoryRepository

    @Binds
    abstract fun bindReflectionCandidateCache(impl: ReflectionCandidateCacheImpl): ReflectionCandidateCache

    @Binds
    abstract fun bindReflectionPersistenceCoordinator(impl: ReflectionPersistenceCoordinatorImpl): com.stoicjournal.core.domain.repository.ReflectionPersistenceCoordinator
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
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
        return Room.databaseBuilder(
            context,
            StoicJournalDatabase::class.java,
            "stoic_journal.db"
        )
            .addMigrations(*StoicJournalMigrations.ALL)
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

    @Provides
    fun provideMentorConversationDao(db: StoicJournalDatabase) = db.mentorConversationDao()

    @Provides
    fun provideMentorMessageDao(db: StoicJournalDatabase) = db.mentorMessageDao()

    @Provides
    fun provideMentorReflectionLinkDao(db: StoicJournalDatabase) = db.mentorReflectionLinkDao()

    @Provides
    fun provideMentorMemoryDao(db: StoicJournalDatabase) = db.mentorMemoryDao()
}
