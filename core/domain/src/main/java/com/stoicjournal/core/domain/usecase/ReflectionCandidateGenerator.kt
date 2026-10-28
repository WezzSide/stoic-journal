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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Singleton
class ReflectionCandidateGenerator @Inject constructor(
    private val providerRegistry: AiProviderRegistry
) {
    suspend fun generateCandidate(
        conversationId: String,
        messages: List<MentorMessage>,
        modelId: String
    ): Result<ReflectionCandidate> {
        val activeProvider = providerRegistry.getActiveProvider()
            ?: return Result.failure(Exception("No active AI provider configured."))

        val prompt = """
            Extract a structured reflection entry from this conversation. Ensure you output valid JSON only. Do not invent information. Do not invent emotions or virtues. If information is missing, leave the field empty.
            Conversation:
            ${messages.joinToString("\n") { "${it.role}: ${it.content}" }}
            
            Return JSON format with exactly these string fields:
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
            selectedModel = activeProvider.resolveModelId(modelId),
            systemInstructionVersion = "1.0",
            authorizedHistoricalContext = "You are a reflection extraction assistant. Output only raw JSON. Do not include markdown blocks.",
            conversationHistory = emptyList(),
            maxOutputTokens = 800,
            cancellationState = false,
            privacySettings = emptyMap(),
            requestCorrelationId = UUID.randomUUID().toString()
        )

        return try {
            val response = activeProvider.generateResponse(request).getOrThrow()
            var text = response.content.trim()
            if (text.startsWith("```json")) {
                text = text.removePrefix("```json").removeSuffix("```").trim()
            } else if (text.startsWith("```")) {
                text = text.removePrefix("```").removeSuffix("```").trim()
            }
            
            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(text).jsonObject
            
            fun getStr(key: String): String = root[key]?.jsonPrimitive?.content ?: ""

            val title = getStr("title").ifBlank { "Daily Reflection" }
            val summary = getStr("summary").ifBlank { "A brief reflection on the day." }

            val candidate = ReflectionCandidate(
                candidateId = UUID.randomUUID().toString(),
                conversationId = conversationId,
                title = title,
                summary = summary,
                whatHappened = getStr("whatHappened"),
                keyInsights = getStr("keyInsights"),
                withinControl = getStr("withinControl"),
                outsideControl = getStr("outsideControl"),
                virtues = emptyList(), // Can be parsed if model is instructed to output them
                nextAction = getStr("nextAction"),
                generatedAt = Instant.now(),
                providerId = activeProvider.providerId,
                modelId = response.model
            )

            Result.success(candidate)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to generate and parse structured reflection", e))
        }
    }
}
