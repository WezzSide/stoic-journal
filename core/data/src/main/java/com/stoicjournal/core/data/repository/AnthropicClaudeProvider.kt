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
class AnthropicClaudeProvider @Inject constructor(
    private val credentialRepository: SecureCredentialRepository,
    private val okHttpClient: OkHttpClient
) : AiProvider {

    override val providerId: String = "claude"
    override val defaultModelId: String = DEFAULT_MODEL

    /** Overridable for mocked-HTTP tests; production always talks to the public endpoint. */
    internal var baseUrl: String = "https://api.anthropic.com"

    private val activeCalls = ConcurrentHashMap<String, Call>()
    private val jsonParser = Json { ignoreUnknownKeys = true }

    internal fun activeCallCount(): Int = activeCalls.size

    private suspend fun getApiKey(): String? {
        val result = credentialRepository.getCredential(providerId)
        val credential = result.getOrNull() as? ProviderCredential.ApiKey
        return credential?.key
    }

    private fun httpRequest(key: String, body: String): Request = Request.Builder()
        .url("$baseUrl/v1/messages")
        .addHeader("x-api-key", key)
        .addHeader("anthropic-version", ANTHROPIC_VERSION)
        .addHeader("content-type", "application/json")
        .post(body.toRequestBody("application/json".toMediaType()))
        .build()

    override suspend fun validateConnection(): Result<Boolean> {
        val key = getApiKey() ?: return Result.failure(Exception("Missing Anthropic API Key"))
        return withContext(Dispatchers.IO) {
            try {
                // Cheapest possible billable probe: current Haiku, one output token.
                val payload = buildJsonObject {
                    put("model", VALIDATION_MODEL)
                    put("max_tokens", 1)
                    putJsonArray("messages") {
                        addJsonObject {
                            put("role", "user")
                            put("content", "ping")
                        }
                    }
                }.toString()

                okHttpClient.newCall(httpRequest(key, payload)).execute().use { response ->
                    when {
                        response.isSuccessful -> Result.success(true)
                        response.code == 401 || response.code == 403 ->
                            Result.failure(Exception(AiError.InvalidCredentials.toString()))
                        else -> Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun listAvailableModels(): Result<List<String>> = Result.success(KNOWN_MODELS)

    private fun buildPayload(request: AiProviderRequest, modelId: String, stream: Boolean): String = buildJsonObject {
        put("model", modelId)
        put("max_tokens", request.maxOutputTokens.coerceAtLeast(256))
        if (stream) put("stream", true)
        if (!request.authorizedHistoricalContext.isNullOrBlank()) {
            put("system", request.authorizedHistoricalContext)
        }
        putJsonArray("messages") {
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

    override suspend fun generateResponse(request: AiProviderRequest): Result<AiProviderResponse> {
        val key = getApiKey() ?: return Result.failure(Exception("Missing Anthropic API Key"))
        val modelId = resolveModelId(request.selectedModel)
        return withContext(Dispatchers.IO) {
            val call = okHttpClient.newCall(httpRequest(key, buildPayload(request, modelId, stream = false)))
            activeCalls[request.requestCorrelationId] = call
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception(classifyHttpError(response.code).toString()))
                    }

                    val bodyStr = response.body?.string() ?: ""
                    val root = jsonParser.parseToJsonElement(bodyStr).jsonObject
                    val contentArr = root["content"]?.jsonArray
                    val text = contentArr
                        ?.map { it.jsonObject }
                        ?.firstOrNull { it["type"]?.jsonPrimitive?.content == "text" }
                        ?.get("text")?.jsonPrimitive?.content
                        ?: ""

                    val usageObj = root["usage"]?.jsonObject
                    val inputTokens = usageObj?.get("input_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val outputTokens = usageObj?.get("output_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0

                    Result.success(
                        AiProviderResponse(
                            content = text,
                            provider = providerId,
                            model = modelId,
                            completionStatus = "completed",
                            usageMetadata = UsageMetadata(inputTokens, outputTokens, 0, null),
                            finishReason = root["stop_reason"]?.jsonPrimitive?.content ?: "end_turn",
                            errorClassification = null
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                activeCalls.remove(request.requestCorrelationId)
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
        val call = okHttpClient.newCall(httpRequest(key, buildPayload(request, modelId, stream = true)))
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
                            try {
                                val root = jsonParser.parseToJsonElement(dataStr).jsonObject
                                val type = root["type"]?.jsonPrimitive?.content
                                if (type == "content_block_delta") {
                                    val delta = root["delta"]?.jsonObject
                                    val text = delta?.get("text")?.jsonPrimitive?.content
                                    if (!text.isNullOrEmpty()) {
                                        accumulatedText += text
                                        emit(AiStreamEvent.TextDelta(text))
                                    }
                                } else if (type == "message_stop") {
                                    break
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
                        finishReason = "end_turn",
                        errorClassification = null
                    )
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(AiStreamEvent.Failed(AiError.Unknown(e.localizedMessage ?: "Stream failed")))
        } finally {
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
        const val ANTHROPIC_VERSION = "2023-06-01"

        /**
         * Current Claude generation (Oct 2026). The previously shipped ids (claude-3-5-sonnet-latest,
         * claude-3-5-haiku-latest, claude-3-7-sonnet-latest, claude-3-opus-20240229) are all retired
         * and return errors from the API.
         */
        const val DEFAULT_MODEL = "claude-opus-5-5"
        const val VALIDATION_MODEL = "claude-haiku-5-5"
        val KNOWN_MODELS = listOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-haiku-5-5")
    }
}
