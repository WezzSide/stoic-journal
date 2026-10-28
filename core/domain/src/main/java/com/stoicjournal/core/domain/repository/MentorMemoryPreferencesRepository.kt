package com.stoicjournal.core.domain.repository

import kotlinx.coroutines.flow.Flow

data class MentorMemoryPreferences(
    val memoryEnabled: Boolean = false,
    val historicalJournalAccessEnabled: Boolean = false,
    val usePreviousConversations: Boolean = false,
    val useApprovedMemories: Boolean = false,
    val sendHistoricalContextToProvider: Boolean = false
)

interface MentorMemoryPreferencesRepository {
    val preferences: Flow<MentorMemoryPreferences>
    suspend fun setMemoryEnabled(enabled: Boolean)
    suspend fun setHistoricalJournalAccessEnabled(enabled: Boolean)
    suspend fun setUsePreviousConversations(enabled: Boolean)
    suspend fun setUseApprovedMemories(enabled: Boolean)
    suspend fun setSendHistoricalContextToProvider(enabled: Boolean)
}
