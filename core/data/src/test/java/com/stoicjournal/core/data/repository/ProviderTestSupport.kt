package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.AiProviderRequest
import com.stoicjournal.core.domain.model.MentorMessage
import com.stoicjournal.core.domain.model.MessageRole
import com.stoicjournal.core.domain.model.ProviderCredential
import com.stoicjournal.core.domain.repository.SecureCredentialRepository
import java.time.Instant

internal class FakeCredentialRepository(private val keys: Map<String, String>) : SecureCredentialRepository {
    override suspend fun saveCredential(providerId: String, credential: ProviderCredential) {}
    override suspend fun getCredential(providerId: String): Result<ProviderCredential> =
        keys[providerId]?.let { Result.success(ProviderCredential.ApiKey(it)) }
            ?: Result.failure(IllegalStateException("MissingCredential"))
    override suspend fun deleteCredential(providerId: String) {}
    override suspend fun hasCredential(providerId: String) = keys.containsKey(providerId)
}

internal fun sampleRequest(
    model: String = "default",
    userMessage: String = "Today I lost my temper in a meeting.",
    system: String? = "SYSTEM_INSTRUCTION_MARKER",
    history: List<MentorMessage> = listOf(
        MentorMessage("m1", "c1", MessageRole.USER, "Earlier user turn", Instant.EPOCH, 1),
        MentorMessage("m2", "c1", MessageRole.MENTOR, "Earlier mentor turn", Instant.EPOCH, 2)
    ),
    correlationId: String = "corr-1"
) = AiProviderRequest(
    conversationId = "c1",
    userMessage = userMessage,
    selectedModel = model,
    systemInstructionVersion = "1.1",
    authorizedHistoricalContext = system,
    conversationHistory = history,
    maxOutputTokens = 500,
    cancellationState = false,
    privacySettings = emptyMap(),
    requestCorrelationId = correlationId
)
