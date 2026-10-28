package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.AiProvider
import com.stoicjournal.core.domain.model.AiProviderRequest
import com.stoicjournal.core.domain.model.AiProviderResponse
import com.stoicjournal.core.domain.model.AiStreamEvent
import com.stoicjournal.core.domain.model.UsageMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic offline provider used by unit and instrumentation tests. It is registered in the
 * production registry but hidden from the provider picker and never selected implicitly.
 */
@Singleton
class FakeAiProvider @Inject constructor() : AiProvider {
    override val providerId: String = PROVIDER_ID
    override val defaultModelId: String = "fake-model-1"

    var shouldFailConnection = false
    var generatedResponseContent = "This is a fake response."
    val cancelledRequests = mutableSetOf<String>()
    var lastRequest: AiProviderRequest? = null
        private set

    override suspend fun validateConnection(): Result<Boolean> {
        return if (shouldFailConnection) Result.failure(Exception("Fake connection failure")) else Result.success(true)
    }

    override suspend fun listAvailableModels(): Result<List<String>> {
        return Result.success(listOf("fake-model-1", "fake-model-2"))
    }

    override suspend fun generateResponse(request: AiProviderRequest): Result<AiProviderResponse> {
        lastRequest = request
        return Result.success(
            AiProviderResponse(
                content = generatedResponseContent,
                provider = providerId,
                model = resolveModelId(request.selectedModel),
                completionStatus = "completed",
                usageMetadata = UsageMetadata(10, 20, 0, null),
                finishReason = "stop",
                errorClassification = null
            )
        )
    }

    override fun streamResponse(request: AiProviderRequest): Flow<Result<AiProviderResponse>> = flow {
        emit(generateResponse(request))
    }

    override fun streamEvents(request: AiProviderRequest): Flow<AiStreamEvent> = flow {
        lastRequest = request
        emit(AiStreamEvent.Started(request.requestCorrelationId))
        emit(AiStreamEvent.TextDelta(generatedResponseContent))
        val response = generateResponse(request).getOrThrow()
        emit(AiStreamEvent.Completed(response))
    }

    override fun cancelRequest(requestId: String) {
        cancelledRequests.add(requestId)
    }

    override fun reportUsage(): UsageMetadata {
        return UsageMetadata(0, 0, 0, null)
    }

    companion object {
        const val PROVIDER_ID = "fake_provider"
    }
}
