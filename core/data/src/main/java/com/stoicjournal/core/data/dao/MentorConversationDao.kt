package com.stoicjournal.core.data.dao

import androidx.room.*
import com.stoicjournal.core.data.entity.MentorConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MentorConversationDao {
    @Query("SELECT * FROM mentor_conversations ORDER BY lastMessageAt DESC")
    fun getAllConversations(): Flow<List<MentorConversationEntity>>

    @Query("SELECT * FROM mentor_conversations WHERE id = :id")
    fun getConversationById(id: String): Flow<MentorConversationEntity?>

    @Query("SELECT * FROM mentor_conversations WHERE id = :id")
    suspend fun getConversationByIdSync(id: String): MentorConversationEntity?

    /**
     * Must be an UPSERT, never `@Insert(onConflict = REPLACE)`: REPLACE deletes the existing row before
     * re-inserting it, which fires `ON DELETE CASCADE` on `mentor_messages` and silently wiped every
     * message of a conversation each time the conversation row was updated (D-34).
     */
    @Upsert
    suspend fun insertOrUpdate(conversation: MentorConversationEntity)

    @Query("DELETE FROM mentor_conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM mentor_conversations")
    suspend fun deleteAll()
}
