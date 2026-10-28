package com.stoicjournal.core.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.stoicjournal.core.domain.model.AiError
import com.stoicjournal.core.domain.model.AiProvider
import com.stoicjournal.core.domain.model.AiProviderRequest
import com.stoicjournal.core.domain.model.AiProviderResponse
import com.stoicjournal.core.domain.model.AiStreamEvent
import com.stoicjournal.core.domain.model.ProviderCredential
import com.stoicjournal.core.domain.model.UsageMetadata
import com.stoicjournal.core.domain.repository.SecureCredentialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiAiProvider @Inject constructor(
    private val credentialRepository: SecureCredentialRepository
) : AiProvider {

    override val providerId: String = "gemini"
    override val defaultModelId: String = DEFAULT_MODEL
    private val activeCalls = ConcurrentHashMap<String, Boolean>()

    private suspend fun getApiKey(): String? {
        val result = credentialRepository.getCredential(providerId)
        val credential = result.getOrNull() as? ProviderCredential.ApiKey
        return credential?.key
    }

    override suspend fun validateConnection(): Result<Boolean> {
        val key = getApiKey() ?: return Result.failure(Exception("Missing Gemini API Key"))
        return try {
            val model = GenerativeModel(
                modelName = DEFAULT_MODEL,
                apiKey = key
            )
            val response = model.generateContent("ping")
            if (!response.text.isNullOrBlank()) {
                Result.success(true)
            } else {
                Result.failure(Exception("Empty Gemini response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listAvailableModels(): Result<List<String>> {
        return Result.success(KNOWN_MODELS)
    }

    override suspend fun generateResponse(request: AiProviderRequest): Result<AiProviderResponse> {
        val key = getApiKey() ?: return Result.failure(Exception("Missing Gemini API Key"))
        return withContext(Dispatchers.IO) {
            try {
                val modelName = resolveModelId(request.selectedModel)
                val model = buildModel(key, modelName, request)

                // Structured multi-turn history so journal/user text can never spoof a role label.
                val chat = model.startChat(history = toHistory(request))
                val response = chat.sendMessage(request.userMessage)
                val responseText = response.text ?: ""

                Result.success(
                    AiProviderResponse(
                        content = responseText,
                        provider = providerId,
                        model = modelName,
                        completionStatus = "completed",
                        usageMetadata = UsageMetadata(0, 0, 0, null),
                        finishReason = "STOP",
                        errorClassification = null
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun streamResponse(request: AiProviderRequest): Flow<Result<AiProviderResponse>> = flow {
        emit(generateResponse(request))
    }

    override fun streamEvents(request: AiProviderRequest): Flow<AiStreamEvent> = flow {
        val key = getApiKey()
        if (key == null) {
            emit(AiStreamEvent.Failed(AiError.MissingCredentials))
            return@flow
        }

        emit(AiStreamEvent.Started(request.requestCorrelationId))
        activeCalls[request.requestCorrelationId] = true

        try {
            val modelName = resolveModelId(request.selectedModel)
            val model = buildModel(key, modelName, request)
            val chat = model.startChat(history = toHistory(request))

            var accumulatedText = ""
            chat.sendMessageStream(request.userMessage).collect { chunk ->
                val text = chunk.text ?: ""
                if (text.isNotEmpty()) {
                    accumulatedText += text
                    emit(AiStreamEvent.TextDelta(text))
                }
            }

            val completedResponse = AiProviderResponse(
                content = accumulatedText,
                provider = providerId,
                model = modelName,
                completionStatus = "completed",
                usageMetadata = UsageMetadata(0, 0, 0, null),
                finishReason = "STOP",
                errorClassification = null
            )
            emit(AiStreamEvent.Completed(completedResponse))
        } catch (e: Exception) {
            emit(AiStreamEvent.Failed(AiError.Unknown(e.localizedMessage ?: "Gemini stream failed")))
        } finally {
            activeCalls.remove(request.requestCorrelationId)
        }
    }.flowOn(Dispatchers.IO)

    private fun buildModel(key: String, modelName: String, request: AiProviderRequest): GenerativeModel {
        val systemPrompt = request.authorizedHistoricalContext ?: "You are a Stoic guide."
        return GenerativeModel(
            modelName = modelName,
            apiKey = key,
            systemInstruction = content { text(systemPrompt) }
        )
    }

    /** Gemini requires alternating user/model turns; the SDK role for the assistant is "model". */
    private fun toHistory(request: AiProviderRequest) = request.conversationHistory.map { msg ->
        content(role = if (msg.role.name == "USER") "user" else "model") { text(msg.content) }
    }

    /**
     * The Gemini SDK streams through a Kotlin Flow; cancelling the collecting coroutine (which the
     * ViewModel does on "Stop generation") cancels the underlying request. Nothing else to do here.
     */
    override fun cancelRequest(requestId: String) {
        activeCalls.remove(requestId)
    }

    override fun reportUsage(): UsageMetadata {
        return UsageMetadata(0, 0, 0, null)
    }

    companion object {
        /**
         * Verified against ai.google.dev/gemini-api/docs/models and /deprecations on 2026-10-28:
         * gemini-2.0-flash was shut down on 2026-06-01 and the 1.5 family is no longer listed.
         */
        const val DEFAULT_MODEL = "gemini-3.8-flash"
        val KNOWN_MODELS = listOf("gemini-3.8-flash", "gemini-3.6-flash", "gemini-3.5-flash-lite", "gemini-3.1-pro-preview")
    }
}
