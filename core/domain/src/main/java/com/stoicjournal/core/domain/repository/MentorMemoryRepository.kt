package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.MentorMemory
import kotlinx.coroutines.flow.Flow

interface MentorMemoryRepository {
    /** Only memories with status APPROVED and an explicit user approval flag. */
    fun getApprovedMemories(): Flow<List<MentorMemory>>

    /** Every non-deleted memory (proposed, awaiting approval, approved, rejected, archived) for review. */
    fun getAllMemories(): Flow<List<MentorMemory>>

    suspend fun saveMemory(memory: MentorMemory)

    /** Marks a memory APPROVED + userApproved; it becomes eligible for retrieval. */
    suspend fun approveMemory(id: String)

    /** Soft-deletes a memory. Never touches the journal entries it was derived from. */
    suspend fun deleteMemory(id: String)

    suspend fun clearAll()
}
