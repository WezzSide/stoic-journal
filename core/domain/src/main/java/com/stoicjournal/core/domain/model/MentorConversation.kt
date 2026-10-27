package com.stoicjournal.core.domain.model

import java.time.Instant

enum class ConversationState {
    CREATED, ACTIVE, WAITING_FOR_PROVIDER, STREAMING_RESPONSE, READY_FOR_REFLECTION, REFLECTION_DRAFTED, AWAITING_APPROVAL, COMPLETED, ARCHIVED, ERROR
}

enum class MessageRole {
    USER, MENTOR, SYSTEM
}

data class MentorMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Instant,
    val sequenceNumber: Int,
    val providerMetadata: String? = null
)

data class MentorConversation(
    val id: String,
    val title: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val providerId: String?,
    val modelId: String?,
    val state: ConversationState,
    val reflectionStatus: String?,
    val linkedJournalEntryId: String?
)
