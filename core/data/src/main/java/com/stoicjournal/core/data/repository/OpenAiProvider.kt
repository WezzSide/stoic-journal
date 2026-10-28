package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.AiError
import com.stoicjournal.core.domain.model.AiProvider
import com.stoicjournal.core.domain.model.AiProviderRequest
import com.stoicjournal.core.domain.model.AiProviderResponse
import com.stoicjournal.core.domain.model.AiStreamEvent
import com.stoicjournal.core.domain.model.ProviderCredential
import com.stoicjournal.core.domain.model.UsageMetadata
import com.stoicjournal.core.domain.repository.SecureCredentialRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiProvider @Inject constructor(
    private val credentialRepository: SecureCredentialRepository,
    private val okHttpClient: OkHttpClient
) : AiProvider {

    override val providerId: String = "openai"
    override val defaultModelId: String = DEFAULT_MODEL

    /** Overridable for mocked-HTTP tests; production always talks to the public endpoint. */
    internal var baseUrl: String = "https://api.openai.com"

    private val activeCalls = ConcurrentHashMap<String, Call>()
    private val jsonParser = Json { ignoreUnknownKeys = true }

    internal fun activeCallCount(): Int = activeCalls.size

    private suspend fun getApiKey(): String? {
        val result = credentialRepository.getCredential(providerId)
        val credential = result.getOrNull() as? ProviderCredential.ApiKey
        return credential?.key
    }

    override suspend fun validateConnection(): Result<Boolean> {
        val key = getApiKey() ?: return Result.failure(Exception("Missing OpenAI API Key"))
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/v1/models")
                    .addHeader("Authorization", "Bearer $key")
                    .get()
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) Result.success(true)
                    else Result.failure(Exception("OpenAI connection failed with HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun listAvailableModels(): Result<List<String>> {
        val key = getApiKey() ?: return Result.failure(Exception("Missing OpenAI API Key"))
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/v1/models")
                    .addHeader("Authorization", "Bearer $key")
                    .get()
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${response.code}"))
                    }
                    val body = response.body?.string() ?: ""
                    val root = jsonParser.parseToJsonElement(body).jsonObject
                    val data = root["data"]?.jsonArray ?: return@withContext Result.success(KNOWN_MODELS)
                    val models = data.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                        .filter { it.startsWith("gpt-") }
                    Result.success(models.ifEmpty { KNOWN_MODELS })
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildBody(request: AiProviderRequest, modelId: String, stream: Boolean): String = buildJsonObject {
        put("model", modelId)
        if (stream) put("stream", true)
        put("store", false)
        putJsonArray("messages") {
            if (!request.authorizedHistoricalContext.isNullOrBlank()) {
                addJsonObject {
                    put("role", "system")
                    put("content", request.authorizedHistoricalContext)
                }
            }
            request.conversationHistory.forEach { msg ->
                addJsonObject {
                    put("role", if (msg.role.name == "USER") "user" else "assistant")
                    put("content", msg.content)
                }
            }
            addJsonObject {
                put("role", "user")
                put("content", request.userMessage)
            }
        }
    }.toString()

    private fun buildHttpRequest(key: String, body: String): Request = Request.Builder()
        .url("$baseUrl/v1/chat/completions")
        .addHeader("Authorization", "Bearer $key")
        .addHeader("Content-Type", "application/json")
        .post(body.toRequestBody("application/json".toMediaType()))
        .build()

    override suspend fun generateResponse(providerRequest: AiProviderRequest): Result<AiProviderResponse> {
        val key = getApiKey() ?: return Result.failure(Exception("Missing OpenAI API Key"))
        val modelId = resolveModelId(providerRequest.selectedModel)
        return withContext(Dispatchers.IO) {
            val call = okHttpClient.newCall(buildHttpRequest(key, buildBody(providerRequest, modelId, stream = false)))
            activeCalls[providerRequest.requestCorrelationId] = call
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorType = classifyHttpError(response.code)
                        return@withContext Result.failure(Exception(errorType.toString()))
                    }

                    val responseStr = response.body?.string() ?: ""
                    val root = jsonParser.parseToJsonElement(responseStr).jsonObject
                    val choices = root["choices"]?.jsonArray
                    val firstChoice = choices?.firstOrNull()?.jsonObject
                    val message = firstChoice?.get("message")?.jsonObject
                    val content = message?.get("content")?.jsonPrimitive?.content ?: ""

                    val usageObj = root["usage"]?.jsonObject
                    val promptTokens = usageObj?.get("prompt_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val completionTokens = usageObj?.get("completion_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0

                    Result.success(
                        AiProviderResponse(
                            content = content,
                            provider = providerId,
                            model = modelId,
                            completionStatus = "completed",
                            usageMetadata = UsageMetadata(promptTokens, completionTokens, 0, null),
                            finishReason = firstChoice?.get("finish_reason")?.jsonPrimitive?.content ?: "stop",
                            errorClassification = null
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                activeCalls.remove(providerRequest.requestCorrelationId)
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

        val modelId = resolveModelId(request.selectedModel)
        val call = okHttpClient.newCall(buildHttpRequest(key, buildBody(request, modelId, stream = true)))
        activeCalls[request.requestCorrelationId] = call
        var response: Response? = null

        try {
            // The flow runs on Dispatchers.IO (flowOn below); blocking I/O is fine here and, crucially,
            // emit() must stay in the flow's own coroutine (no nested withContext) or the Flow invariant
            // "emission from another coroutine" is violated and the stream dies after the first delta.
            response = call.execute()
            if (!response.isSuccessful) {
                emit(AiStreamEvent.Failed(classifyHttpError(response.code)))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(AiStreamEvent.Failed(AiError.InvalidResponse))
                return@flow
            }

            var accumulatedText = ""
            BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                run {
                    var line = reader.readLine()
                    while (line != null) {
                        if (line.startsWith("data: ")) {
                            val dataStr = line.removePrefix("data: ").trim()
                            if (dataStr == "[DONE]") break
                            try {
                                val root = jsonParser.parseToJsonElement(dataStr).jsonObject
                                val choices = root["choices"]?.jsonArray
                                val delta = choices?.firstOrNull()?.jsonObject?.get("delta")?.jsonObject
                                val textChunk = delta?.get("content")?.jsonPrimitive?.content
                                if (!textChunk.isNullOrEmpty()) {
                                    accumulatedText += textChunk
                                    emit(AiStreamEvent.TextDelta(textChunk))
                                }
                            } catch (_: kotlinx.serialization.SerializationException) {
                            } catch (_: IllegalArgumentException) {
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }

            emit(
                AiStreamEvent.Completed(
                    AiProviderResponse(
                        content = accumulatedText,
                        provider = providerId,
                        model = modelId,
                        completionStatus = "completed",
                        usageMetadata = UsageMetadata(0, 0, 0, null),
                        finishReason = "stop",
                        errorClassification = null
                    )
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(AiStreamEvent.Failed(AiError.Unknown(e.localizedMessage ?: "Stream failed")))
        } finally {
            // Always release the socket: no-op after a completed response, closes the connection when the
            // collector was cancelled mid-stream ("Stop generation") so no paid tokens keep flowing.
            activeCalls.remove(request.requestCorrelationId)
            call.cancel()
            response?.close()
        }
    }.flowOn(Dispatchers.IO)

    private fun classifyHttpError(code: Int): AiError = when (code) {
        401, 403 -> AiError.InvalidCredentials
        429 -> AiError.RateLimited
        else -> AiError.ProviderUnavailable
    }

    override fun cancelRequest(requestId: String) {
        activeCalls.remove(requestId)?.cancel()
    }

    override fun reportUsage(): UsageMetadata {
        return UsageMetadata(0, 0, 0, null)
    }

    companion object {
        /** Verified against developers.openai.com/api/docs/models on 2026-10-28. */
        const val DEFAULT_MODEL = "gpt-6.1-sol"
        val KNOWN_MODELS = listOf("gpt-6.1-sol", "gpt-6-astra", "gpt-6.1-terra", "gpt-6-luna")
    }
}
