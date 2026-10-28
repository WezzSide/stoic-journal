package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.MentorMemory
import kotlinx.coroutines.flow.Flow

interface MentorMemoryRepository {
    fun getApprovedMemories(): Flow<List<MentorMemory>>
    suspend fun saveMemory(memory: MentorMemory)
    suspend fun deleteMemory(id: String)
    suspend fun clearAll()
}
