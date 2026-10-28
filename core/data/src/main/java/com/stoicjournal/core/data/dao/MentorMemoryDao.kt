package com.stoicjournal.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stoicjournal.core.data.entity.MentorMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MentorMemoryDao {
    @Query("SELECT * FROM mentor_memories WHERE status = 'APPROVED' AND userApproved = 1 ORDER BY updatedAt DESC")
    fun getApprovedMemories(): Flow<List<MentorMemoryEntity>>

    @Query("SELECT * FROM mentor_memories WHERE status != 'DELETED' ORDER BY updatedAt DESC")
    fun getAllMemories(): Flow<List<MentorMemoryEntity>>

    @Query("SELECT * FROM mentor_memories WHERE id = :id")
    suspend fun getMemoryById(id: String): MentorMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(memory: MentorMemoryEntity)

    @Query("UPDATE mentor_memories SET status = 'APPROVED', userApproved = 1, lastReviewedAt = :reviewedAt, updatedAt = :reviewedAt WHERE id = :id")
    suspend fun approveMemory(id: String, reviewedAt: Long)

    @Query("UPDATE mentor_memories SET status = 'DELETED', userApproved = 0, updatedAt = :deletedAt WHERE id = :id")
    suspend fun deleteMemory(id: String, deletedAt: Long)

    @Query("DELETE FROM mentor_memories")
    suspend fun deleteAll()
}
