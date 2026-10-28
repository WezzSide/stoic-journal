package com.stoicjournal.core.data.dao

import androidx.room.*
import com.stoicjournal.core.data.entity.MentorMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MentorMemoryDao {
    @Query("SELECT * FROM mentor_memories WHERE status = 'APPROVED' AND userApproved = 1 ORDER BY updatedAt DESC")
    fun getApprovedMemories(): Flow<List<MentorMemoryEntity>>

    @Query("SELECT * FROM mentor_memories WHERE id = :id")
    suspend fun getMemoryById(id: String): MentorMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(memory: MentorMemoryEntity)

    @Query("UPDATE mentor_memories SET status = 'DELETED' WHERE id = :id")
    suspend fun deleteMemory(id: String)

    @Query("DELETE FROM mentor_memories")
    suspend fun deleteAll()
}
