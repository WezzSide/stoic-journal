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
class OpenAiProvider @Inject constructor(
    private val credentialRepository: SecureCredentialRepository,
    private val okHttpClient: OkHttpClient
) : AiProvider {

    override val providerId: String = "openai"
    private val activeCalls = ConcurrentHashMap<String, okhttp3.Call>()

    private val jsonParser = Json { ignoreUnknownKeys = true }

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
                    .url("https://api.openai.com/v1/models")
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
                    .url("https://api.openai.com/v1/models")
                    .addHeader("Authorization", "Bearer $key")
                    .get()
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${response.code}"))
                    }
                    val body = response.body?.string() ?: ""
                    val root = jsonParser.parseToJsonElement(body).jsonObject
                    val data = root["data"]?.jsonArray ?: return@withContext Result.success(listOf("gpt-4o", "gpt-4o-mini"))
                    val models = data.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                        .filter { it.startsWith("gpt-") }
                    Result.success(models.ifEmpty { listOf("gpt-4o", "gpt-4o-mini") })
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun generateResponse(providerRequest: AiProviderRequest): Result<AiProviderResponse> {
        val key = getApiKey() ?: return Result.failure(Exception("Missing OpenAI API Key"))
        return withContext(Dispatchers.IO) {
            try {
                val requestBodyJson = buildJsonObject {
                    put("model", providerRequest.selectedModel)
                    put("store", false)
                    putJsonArray("messages") {
                        if (providerRequest.authorizedHistoricalContext != null) {
                            addJsonObject {
                                put("role", "system")
                                put("content", providerRequest.authorizedHistoricalContext)
                            }
                        }
                        providerRequest.conversationHistory.forEach { msg ->
                            addJsonObject {
                                put("role", if (msg.role.name == "USER") "user" else "assistant")
                                put("content", msg.content)
                            }
                        }
                        addJsonObject {
                            put("role", "user")
                            put("content", providerRequest.userMessage)
                        }
                    }
                }.toString()

                val request = Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $key")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
                    .build()

                val call = okHttpClient.newCall(request)
                activeCalls[providerRequest.requestCorrelationId] = call

                call.execute().use { response ->
                    activeCalls.remove(providerRequest.requestCorrelationId)
                    if (!response.isSuccessful) {
                        val code = response.code
                        val errorType = when (code) {
                            401 -> AiError.InvalidCredentials
                            429 -> AiError.RateLimited
                            else -> AiError.Unknown("HTTP_$code")
                        }
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
                            model = providerRequest.selectedModel,
                            completionStatus = "completed",
                            usageMetadata = UsageMetadata(promptTokens, completionTokens, 0, null),
                            finishReason = firstChoice?.get("finish_reason")?.jsonPrimitive?.content ?: "stop",
                            errorClassification = null
                        )
                    )
                }
            } catch (e: Exception) {
                activeCalls.remove(providerRequest.requestCorrelationId)
                Result.failure(e)
            }
        }
    }

    override fun streamResponse(request: AiProviderRequest): Flow<Result<AiProviderResponse>> = flow {
        val result = generateResponse(request)
        emit(result)
    }

    override fun streamEvents(request: AiProviderRequest): Flow<AiStreamEvent> = flow {
        val key = getApiKey()
        if (key == null) {
            emit(AiStreamEvent.Failed(AiError.MissingCredentials))
            return@flow
        }

        emit(AiStreamEvent.Started(request.requestCorrelationId))

        val requestBodyJson = buildJsonObject {
            put("model", request.selectedModel)
            put("stream", true)
            put("store", false)
            putJsonArray("messages") {
                if (request.authorizedHistoricalContext != null) {
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

        val httpRequest = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
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
                finishReason = "stop",
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
