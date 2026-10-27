package com.stoicjournal.core.domain.model

import kotlinx.coroutines.flow.Flow

data class AiProviderRequest(
    val conversationId: String,
    val userMessage: String,
    val selectedModel: String,
    val systemInstructionVersion: String,
    val authorizedHistoricalContext: String?,
    val conversationHistory: List<MentorMessage>,
    val maxOutputTokens: Int,
    val cancellationState: Boolean,
    val privacySettings: Map<String, Boolean>,
    val requestCorrelationId: String
)

data class AiProviderResponse(
    val content: String,
    val provider: String,
    val model: String,
    val completionStatus: String,
    val usageMetadata: UsageMetadata,
    val finishReason: String,
    val errorClassification: AiError?
)

data class UsageMetadata(
    val inputTokens: Int,
    val outputTokens: Int,
    val cachedTokens: Int,
    val estimatedMonetaryCost: Double?
)

sealed interface AiStreamEvent {
    data class Started(val requestId: String) : AiStreamEvent
    data class TextDelta(val text: String) : AiStreamEvent
    data class UsageUpdated(val usage: UsageMetadata) : AiStreamEvent
    data class Completed(val response: AiProviderResponse) : AiStreamEvent
    data class Failed(val error: AiError) : AiStreamEvent
}

interface AiProvider {
    val providerId: String
    suspend fun validateConnection(): Result<Boolean>
    suspend fun listAvailableModels(): Result<List<String>>
    suspend fun generateResponse(request: AiProviderRequest): Result<AiProviderResponse>
    fun streamResponse(request: AiProviderRequest): Flow<Result<AiProviderResponse>>
    fun streamEvents(request: AiProviderRequest): Flow<AiStreamEvent>
    fun cancelRequest(requestId: String)
    fun reportUsage(): UsageMetadata
}

interface AiProviderRegistry {
    fun registerProvider(provider: AiProvider)
    fun getSupportedProviders(): List<AiProvider>
    fun getProvider(providerId: String): AiProvider?
    fun getActiveProvider(): AiProvider?
}
