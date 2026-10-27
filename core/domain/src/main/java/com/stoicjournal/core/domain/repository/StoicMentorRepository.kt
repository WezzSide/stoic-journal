package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.MentorConversation
import com.stoicjournal.core.domain.model.MentorMessage
import kotlinx.coroutines.flow.Flow

interface StoicMentorRepository {
    suspend fun saveConversation(conversation: MentorConversation)
    fun getConversation(conversationId: String): Flow<MentorConversation?>
    fun getAllConversations(): Flow<List<MentorConversation>>
    suspend fun deleteConversation(conversationId: String)
    
    suspend fun saveMessage(message: MentorMessage)
    fun getMessagesForConversation(conversationId: String): Flow<List<MentorMessage>>
    
    suspend fun clearAll()
}
