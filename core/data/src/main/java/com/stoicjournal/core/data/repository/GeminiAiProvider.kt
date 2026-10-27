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
                modelName = "gemini-1.5-flash",
                apiKey = key
            )
            val response = model.generateContent("hi")
            if (!response.text.isNull_or_blank()) {
                Result.success(true)
            } else {
                Result.failure(Exception("Empty Gemini response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listAvailableModels(): Result<List<String>> {
        return Result.success(
            listOf(
                "gemini-1.5-pro",
                "gemini-1.5-flash",
                "gemini-1.0-pro"
            )
        )
    }

    override suspend fun generateResponse(request: AiProviderRequest): Result<AiProviderResponse> {
        val key = getApiKey() ?: return Result.failure(Exception("Missing Gemini API Key"))
        return withContext(Dispatchers.IO) {
            try {
                val modelName = request.selectedModel.ifBlank { "gemini-1.5-flash" }
                val systemPrompt = request.authorizedHistoricalContext ?: "You are a Stoic guide."
                val model = GenerativeModel(
                    modelName = modelName,
                    apiKey = key,
                    systemInstruction = content { text(systemPrompt) }
                )

                val prompt = buildString {
                    request.conversationHistory.forEach { msg ->
                        append("${msg.role.name}: ${msg.content}\n")
                    }
                    append("USER: ${request.userMessage}")
                }

                val response = model.generateContent(prompt)
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

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

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
            val modelName = request.selectedModel.ifBlank { "gemini-1.5-flash" }
            val systemPrompt = request.authorizedHistoricalContext ?: "You are a Stoic guide."
            val model = GenerativeModel(
                modelName = modelName,
                apiKey = key,
                systemInstruction = content { text(systemPrompt) }
            )

            val prompt = buildString {
                request.conversationHistory.forEach { msg ->
                    append("${msg.role.name}: ${msg.content}\n")
                }
                append("USER: ${request.userMessage}")
            }

            var accumulatedText = ""
            model.generateContentStream(prompt).collect { chunk ->
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

    override fun cancelRequest(requestId: String) {
        activeCalls.remove(requestId)
    }

    override fun reportUsage(): UsageMetadata {
        return UsageMetadata(0, 0, 0, null)
    }
}
