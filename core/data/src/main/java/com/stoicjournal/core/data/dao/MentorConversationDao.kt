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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: MentorConversationEntity)

    @Query("DELETE FROM mentor_conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM mentor_conversations")
    suspend fun deleteAll()
}
