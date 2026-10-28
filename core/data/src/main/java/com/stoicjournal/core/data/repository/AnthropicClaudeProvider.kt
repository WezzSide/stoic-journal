package com.stoicjournal.core.data.repository

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
    private val activeCalls = ConcurrentHashMap<String, Call>()
    private val jsonParser = Json { ignoreUnknownKeys = true }

    private suspend fun getApiKey(): String? {
        val result = credentialRepository.getCredential(providerId)
        val credential = result.getOrNull() as? ProviderCredential.ApiKey
        return credential?.key
    }

    override suspend fun validateConnection(): Result<Boolean> {
        val key = getApiKey() ?: return Result.failure(Exception("Missing Anthropic API Key"))
        return withContext(Dispatchers.IO) {
            try {
                val dummyPayload = buildJsonObject {
                    put("model", "claude-3-5-haiku-latest")
                    put("max_tokens", 1)
                    putJsonArray("messages") {
                        addJsonObject {
                            put("role", "user")
                            put("content", "ping")
                        }
                    }
                }.toString()

                val request = Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .addHeader("x-api-key", key)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .post(dummyPayload.toRequestBody("application/json".toMediaType()))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Result.success(true)
                    } else if (response.code == 401) {
                        Result.failure(Exception(AiError.InvalidCredentials.toString()))
                    } else {
                        Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun listAvailableModels(): Result<List<String>> {
        return Result.success(
            listOf(
                "claude-3-5-sonnet-latest",
                "claude-3-5-haiku-latest",
                "claude-3-7-sonnet-latest",
                "claude-3-opus-20240229"
            )
        )
    }

    override suspend fun generateResponse(request: AiProviderRequest): Result<AiProviderResponse> {
        val key = getApiKey() ?: return Result.failure(Exception("Missing Anthropic API Key"))
        return withContext(Dispatchers.IO) {
            try {
                val payload = buildJsonObject {
                    put("model", request.selectedModel.ifBlank { "claude-3-5-sonnet-latest" })
                    put("max_tokens", request.maxOutputTokens.coerceAtLeast(256))
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

                val httpRequest = Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .addHeader("x-api-key", key)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()

                val call = okHttpClient.newCall(httpRequest)
                activeCalls[request.requestCorrelationId] = call

                call.execute().use { response ->
                    activeCalls.remove(request.requestCorrelationId)
                    if (!response.isSuccessful) {
                        val error = when (response.code) {
                            401 -> AiError.InvalidCredentials
                            429 -> AiError.RateLimited
                            else -> AiError.Unknown("HTTP_${response.code}")
                        }
                        return@withContext Result.failure(Exception(error.toString()))
                    }

                    val bodyStr = response.body?.string() ?: ""
                    val root = jsonParser.parseToJsonElement(bodyStr).jsonObject
                    val contentArr = root["content"]?.jsonArray
                    val firstContent = contentArr?.firstOrNull()?.jsonObject
                    val text = firstContent?.get("text")?.jsonPrimitive?.content ?: ""

                    val usageObj = root["usage"]?.jsonObject
                    val inputTokens = usageObj?.get("input_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val outputTokens = usageObj?.get("output_tokens")?.jsonPrimitive?.content?.toIntOrNull() ?: 0

                    Result.success(
                        AiProviderResponse(
                            content = text,
                            provider = providerId,
                            model = request.selectedModel,
                            completionStatus = "completed",
                            usageMetadata = UsageMetadata(inputTokens, outputTokens, 0, null),
                            finishReason = root["stop_reason"]?.jsonPrimitive?.content ?: "end_turn",
                            errorClassification = null
                        )
                    )
                }
            } catch (e: Exception) {
                activeCalls.remove(request.requestCorrelationId)
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

        val payload = buildJsonObject {
            put("model", request.selectedModel.ifBlank { "claude-3-5-sonnet-latest" })
            put("max_tokens", request.maxOutputTokens.coerceAtLeast(256))
            put("stream", true)
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

        val httpRequest = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", key)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val call = okHttpClient.newCall(httpRequest)
        activeCalls[request.requestCorrelationId] = call

        try {
            val response = withContext(Dispatchers.IO) { call.execute() }
            if (!response.isSuccessful) {
                val error = when (response.code) {
                    401 -> AiError.InvalidCredentials
                    429 -> AiError.RateLimited
                    else -> AiError.ProviderUnavailable
                }
                emit(AiStreamEvent.Failed(error))
                activeCalls.remove(request.requestCorrelationId)
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(AiStreamEvent.Failed(AiError.InvalidResponse))
                activeCalls.remove(request.requestCorrelationId)
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var accumulatedText = ""

            withContext(Dispatchers.IO) {
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
                            }
                        } catch (_: Exception) {}
                    }
                    line = reader.readLine()
                }
            }

            val completedResponse = AiProviderResponse(
                content = accumulatedText,
                provider = providerId,
                model = request.selectedModel,
                completionStatus = "completed",
                usageMetadata = UsageMetadata(0, 0, 0, null),
                finishReason = "end_turn",
                errorClassification = null
            )
            emit(AiStreamEvent.Completed(completedResponse))
        } catch (e: Exception) {
            emit(AiStreamEvent.Failed(AiError.Unknown(e.localizedMessage ?: "Stream failed")))
        } finally {
            activeCalls.remove(request.requestCorrelationId)
        }
    }.flowOn(Dispatchers.IO)

    override fun cancelRequest(requestId: String) {
        activeCalls[requestId]?.cancel()
        activeCalls.remove(requestId)
    }

    override fun reportUsage(): UsageMetadata {
        return UsageMetadata(0, 0, 0, null)
    }
}
