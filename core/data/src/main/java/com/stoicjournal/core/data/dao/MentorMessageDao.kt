package com.stoicjournal.core.data.dao

import androidx.room.*
import com.stoicjournal.core.data.entity.MentorMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MentorMessageDao {
    @Query("SELECT * FROM mentor_messages WHERE conversationId = :conversationId ORDER BY sequenceNumber ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MentorMessageEntity>>

    /**
     * Must be an UPSERT, never `@Insert(onConflict = REPLACE)`: REPLACE deletes the existing row before
     * re-inserting it, which fires `ON DELETE CASCADE` on `mentor_messages` and silently wiped every
     * message of a conversation each time the conversation row was updated (D-34).
     */
    @Upsert
    suspend fun insertOrUpdate(message: MentorMessageEntity)

    @Query("DELETE FROM mentor_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)
}
