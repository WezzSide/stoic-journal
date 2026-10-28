package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.AiProviderRegistry
import com.stoicjournal.core.domain.model.AiProviderRequest
import com.stoicjournal.core.domain.model.MentorMessage
import com.stoicjournal.core.domain.model.ReflectionCandidate
import com.stoicjournal.core.domain.model.Virtue
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReflectionCandidateGenerator @Inject constructor(
    private val providerRegistry: AiProviderRegistry
) {
    suspend fun generateCandidate(
        conversationId: String,
        messages: List<MentorMessage>
    ): Result<ReflectionCandidate> {
        val activeProvider = providerRegistry.getActiveProvider()
            ?: return Result.failure(Exception("No active AI provider configured."))

        val prompt = """
            Extract a structured reflection entry from this conversation:
            ${messages.joinToString("\n") { "${it.role}: ${it.content}" }}
            
            Return JSON format with:
            {
              "title": "Title summary",
              "summary": "Key summary",
              "whatHappened": "Description",
              "keyInsights": "Insights",
              "withinControl": "Controllable actions",
              "outsideControl": "External factors",
              "nextAction": "Intended action"
            }
        """.trimIndent()

        val request = AiProviderRequest(
            conversationId = conversationId,
            userMessage = prompt,
            selectedModel = "default",
            systemInstructionVersion = "1.0",
            authorizedHistoricalContext = "You are a reflection extraction assistant.",
            conversationHistory = emptyList(),
            maxOutputTokens = 500,
            cancellationState = false,
            privacySettings = emptyMap(),
            requestCorrelationId = UUID.randomUUID().toString()
        )

        return try {
            val response = activeProvider.generateResponse(request).getOrThrow()
            val text = response.content.ifBlank { "Daily Stoic Reflection" }

            val candidate = ReflectionCandidate(
                candidateId = UUID.randomUUID().toString(),
                conversationId = conversationId,
                title = "Reflection: " + messages.lastOrNull()?.content?.take(25)?.ifBlank { "Daily Practice" },
                summary = text,
                whatHappened = messages.firstOrNull()?.content ?: "",
                keyInsights = text,
                withinControl = "Focus on choices and communication",
                outsideControl = "External circumstances and outcomes",
                virtues = listOf(Virtue.WISDOM, Virtue.TEMPERANCE),
                nextAction = "Practice reflection daily",
                generatedAt = Instant.now(),
                providerId = activeProvider.providerId,
                modelId = response.model
            )

            Result.success(candidate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
