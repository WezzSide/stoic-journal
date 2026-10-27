package com.stoicjournal.core.domain.model

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

interface AiProvider {
    val providerId: String
    suspend fun validateConnection(): Result<Boolean>
    suspend fun listAvailableModels(): Result<List<String>>
    suspend fun generateResponse(request: AiProviderRequest): Result<AiProviderResponse>
    fun streamResponse(request: AiProviderRequest): kotlinx.coroutines.flow.Flow<Result<AiProviderResponse>>
    fun cancelResponse()
    fun reportUsage(): UsageMetadata
}

interface AiProviderRegistry {
    fun registerProvider(provider: AiProvider)
    fun getSupportedProviders(): List<AiProvider>
    fun getProvider(providerId: String): AiProvider?
    fun getActiveProvider(): AiProvider?
}
