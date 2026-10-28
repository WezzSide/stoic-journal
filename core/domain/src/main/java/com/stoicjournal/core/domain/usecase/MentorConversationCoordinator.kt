package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.AiError
import com.stoicjournal.core.domain.model.AiProviderRegistry
import com.stoicjournal.core.domain.model.AiProviderRequest
import com.stoicjournal.core.domain.model.AiStreamEvent
import com.stoicjournal.core.domain.model.ConversationState
import com.stoicjournal.core.domain.model.MentorConversation
import com.stoicjournal.core.domain.model.MentorConversationStateMachine
import com.stoicjournal.core.domain.model.MentorEvent
import com.stoicjournal.core.domain.model.MentorMessage
import com.stoicjournal.core.domain.model.MessageRole
import com.stoicjournal.core.domain.repository.StoicMentorRepository
import com.stoicjournal.core.domain.util.MentorInstructionBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MentorConversationCoordinator @Inject constructor(
    private val mentorRepository: StoicMentorRepository,
    private val providerRegistry: AiProviderRegistry,
    private val historicalReflectionRetriever: HistoricalReflectionRetriever
) {
    fun sendMessage(
        conversationId: String,
        userContent: String,
        mode: String = "DAILY_REFLECTION"
    ): Flow<AiStreamEvent> = flow {
        val activeProvider = providerRegistry.getActiveProvider()
        if (activeProvider == null) {
            emit(AiStreamEvent.Failed(AiError.ProviderUnavailable))
            return@flow
        }

        var conversation = mentorRepository.getConversation(conversationId).first()
        if (conversation == null) {
            conversation = MentorConversation(
                id = conversationId,
                title = userContent.take(30),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                providerId = activeProvider.providerId,
                modelId = "default",
                state = ConversationState.ACTIVE,
                reflectionStatus = null,
                linkedJournalEntryId = null
            )
            mentorRepository.saveConversation(conversation)
        }

        val existingMessages = mentorRepository.getMessagesForConversation(conversationId).first()
        val userMessage = MentorMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = MessageRole.USER,
            content = userContent,
            timestamp = Instant.now(),
            sequenceNumber = existingMessages.size + 1
        )
        mentorRepository.saveMessage(userMessage)

        val stateMachine = MentorConversationStateMachine(conversation.state)
        stateMachine.transition(MentorEvent.UserSentMessage)

        val updatedHistory = existingMessages // Removed userMessage to prevent duplication!
        val correlationId = UUID.randomUUID().toString()

        val contextResult = historicalReflectionRetriever.retrieveContext(userContent)
        val systemInstruction = MentorInstructionBuilder.buildSystemInstruction(mode)
        val authorizedContext = buildString {
            append(systemInstruction)
            if (contextResult.formattedContextString.isNotBlank()) {
                append("\n\n")
                append(contextResult.formattedContextString)
            }
        }

        val request = AiProviderRequest(
            conversationId = conversationId,
            userMessage = userContent,
            selectedModel = conversation.modelId ?: "default",
            systemInstructionVersion = "1.1",
            authorizedHistoricalContext = authorizedContext,
            conversationHistory = updatedHistory,
            maxOutputTokens = 500,
            cancellationState = false,
            privacySettings = emptyMap(),
            requestCorrelationId = correlationId
        )

        val assistantMessageId = UUID.randomUUID().toString()
        var accumulatedText = ""

        activeProvider.streamEvents(request).collect { event ->
            when (event) {
                is AiStreamEvent.Started -> {
                    stateMachine.transition(MentorEvent.StartStreaming)
                    emit(event)
                }
                is AiStreamEvent.TextDelta -> {
                    accumulatedText += event.text
                    emit(event)
                }
                is AiStreamEvent.Completed -> {
                    stateMachine.transition(MentorEvent.StreamCompleted)
                    val mentorMessage = MentorMessage(
                        id = assistantMessageId,
                        conversationId = conversationId,
                        role = MessageRole.MENTOR,
                        content = event.response.content.ifBlank { accumulatedText },
                        timestamp = Instant.now(),
                        sequenceNumber = existingMessages.size + 2,
                        providerMetadata = correlationId
                    )
                    mentorRepository.saveMessage(mentorMessage)
                    mentorRepository.saveConversation(conversation.copy(updatedAt = Instant.now(), state = ConversationState.ACTIVE))
                    emit(event)
                }
                is AiStreamEvent.Failed -> {
                    stateMachine.transition(MentorEvent.ErrorOccurred(event.error))
                    emit(event)
                }
                is AiStreamEvent.UsageUpdated -> {
                    emit(event)
                }
            }
        }
    }
}
