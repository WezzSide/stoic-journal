package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.MentorConversationDao
import com.stoicjournal.core.data.dao.MentorMessageDao
import com.stoicjournal.core.data.entity.MentorConversationEntity
import com.stoicjournal.core.data.entity.MentorMessageEntity
import com.stoicjournal.core.domain.model.ConversationState
import com.stoicjournal.core.domain.model.MentorConversation
import com.stoicjournal.core.domain.model.MentorMessage
import com.stoicjournal.core.domain.model.MessageRole
import com.stoicjournal.core.domain.repository.StoicMentorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MentorConversationRepositoryImpl @Inject constructor(
    private val conversationDao: MentorConversationDao,
    private val messageDao: MentorMessageDao
) : StoicMentorRepository {

    override suspend fun saveConversation(conversation: MentorConversation) {
        val entity = MentorConversationEntity(
            id = conversation.id,
            title = conversation.title,
            createdAt = conversation.createdAt.toEpochMilli(),
            updatedAt = conversation.updatedAt.toEpochMilli(),
            providerId = conversation.providerId,
            modelId = conversation.modelId,
            conversationMode = "DAILY_REFLECTION",
            status = conversation.state.name,
            lastMessageAt = conversation.updatedAt.toEpochMilli(),
            linkedJournalEntryId = conversation.linkedJournalEntryId
        )
        conversationDao.insertOrUpdate(entity)
    }

    override fun getConversation(conversationId: String): Flow<MentorConversation?> {
        return conversationDao.getConversationById(conversationId).map { entity ->
            entity?.toDomainModel()
        }
    }

    override fun getAllConversations(): Flow<List<MentorConversation>> {
        return conversationDao.getAllConversations().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteById(conversationId)
    }

    override suspend fun saveMessage(message: MentorMessage) {
        val entity = MentorMessageEntity(
            id = message.id,
            conversationId = message.conversationId,
            role = message.role.name,
            content = message.content,
            createdAt = message.timestamp.toEpochMilli(),
            sequenceNumber = message.sequenceNumber,
            deliveryStatus = "DELIVERED",
            requestCorrelationId = message.providerMetadata
        )
        messageDao.insertOrUpdate(entity)
    }

    override fun getMessagesForConversation(conversationId: String): Flow<List<MentorMessage>> {
        return messageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun clearAll() {
        conversationDao.deleteAll()
    }

    private fun MentorConversationEntity.toDomainModel(): MentorConversation {
        return MentorConversation(
            id = id,
            title = title,
            createdAt = Instant.ofEpochMilli(createdAt),
            updatedAt = Instant.ofEpochMilli(updatedAt),
            providerId = providerId,
            modelId = modelId,
            state = try { ConversationState.valueOf(status) } catch (_: Exception) { ConversationState.ACTIVE },
            reflectionStatus = null,
            linkedJournalEntryId = linkedJournalEntryId
        )
    }

    private fun MentorMessageEntity.toDomainModel(): MentorMessage {
        return MentorMessage(
            id = id,
            conversationId = conversationId,
            role = try { MessageRole.valueOf(role) } catch (_: Exception) { MessageRole.USER },
            content = content,
            timestamp = Instant.ofEpochMilli(createdAt),
            sequenceNumber = sequenceNumber,
            providerMetadata = requestCorrelationId
        )
    }
}
