package com.stoicjournal.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.stoicjournal.core.domain.repository.MentorMemoryPreferences
import com.stoicjournal.core.domain.repository.MentorMemoryPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.memoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "mentor_memory_preferences")

@Singleton
class MentorMemoryPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MentorMemoryPreferencesRepository {

    private object Keys {
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val HISTORICAL_JOURNAL_ACCESS = booleanPreferencesKey("historical_journal_access")
        val USE_PREVIOUS_CONVERSATIONS = booleanPreferencesKey("use_previous_conversations")
        val USE_APPROVED_MEMORIES = booleanPreferencesKey("use_approved_memories")
        val SEND_HISTORICAL_CONTEXT = booleanPreferencesKey("send_historical_context")
    }

    override val preferences: Flow<MentorMemoryPreferences> = context.memoryDataStore.data.map { prefs ->
        MentorMemoryPreferences(
            memoryEnabled = prefs[Keys.MEMORY_ENABLED] ?: false,
            historicalJournalAccessEnabled = prefs[Keys.HISTORICAL_JOURNAL_ACCESS] ?: false,
            usePreviousConversations = prefs[Keys.USE_PREVIOUS_CONVERSATIONS] ?: false,
            useApprovedMemories = prefs[Keys.USE_APPROVED_MEMORIES] ?: false,
            sendHistoricalContextToProvider = prefs[Keys.SEND_HISTORICAL_CONTEXT] ?: false
        )
    }

    override suspend fun setMemoryEnabled(enabled: Boolean) {
        context.memoryDataStore.edit { it[Keys.MEMORY_ENABLED] = enabled }
    }

    override suspend fun setHistoricalJournalAccessEnabled(enabled: Boolean) {
        context.memoryDataStore.edit { it[Keys.HISTORICAL_JOURNAL_ACCESS] = enabled }
    }

    override suspend fun setUsePreviousConversations(enabled: Boolean) {
        context.memoryDataStore.edit { it[Keys.USE_PREVIOUS_CONVERSATIONS] = enabled }
    }

    override suspend fun setUseApprovedMemories(enabled: Boolean) {
        context.memoryDataStore.edit { it[Keys.USE_APPROVED_MEMORIES] = enabled }
    }

    override suspend fun setSendHistoricalContextToProvider(enabled: Boolean) {
        context.memoryDataStore.edit { it[Keys.SEND_HISTORICAL_CONTEXT] = enabled }
    }
}
