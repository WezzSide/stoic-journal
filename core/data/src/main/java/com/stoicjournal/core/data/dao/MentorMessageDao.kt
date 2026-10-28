package com.stoicjournal.core.data.dao

import androidx.room.*
import com.stoicjournal.core.data.entity.MentorMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MentorMessageDao {
    @Query("SELECT * FROM mentor_messages WHERE conversationId = :conversationId ORDER BY sequenceNumber ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MentorMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(message: MentorMessageEntity)

    @Query("DELETE FROM mentor_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)
}
