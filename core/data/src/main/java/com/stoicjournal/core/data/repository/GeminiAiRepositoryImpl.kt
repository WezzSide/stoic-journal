package com.stoicjournal.core.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import android.util.Log
import com.stoicjournal.core.domain.model.AiReflectionRequest
import com.stoicjournal.core.domain.model.AiReflectionResponse
import com.stoicjournal.core.domain.model.AiReflectionTheme
import com.stoicjournal.core.domain.repository.AiRepository
import com.stoicjournal.core.domain.util.StoicPromptTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiAiRepositoryImpl @Inject constructor() : AiRepository {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.1-pro-preview",
        apiKey = "dummy_key",
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        },
        systemInstruction = content {
            text(StoicPromptTemplate.buildSystemPrompt())
        }
    )

    override suspend fun getReflection(request: AiReflectionRequest): Result<AiReflectionResponse> {
        return try {
            val apiKey = "dummy_key"
            if (apiKey == "YOUR_API_KEY_HERE" || apiKey.isBlank()) {
                return Result.failure(Exception("Missing Gemini API Key. Please add it to local.properties."))
            }

            val prompt = StoicPromptTemplate.buildUserPrompt(request)
            val response = generativeModel.generateContent(prompt)
            val text = response.text ?: return Result.failure(Exception("Empty response"))
            
            val parsedResponse = parseResponse(text, request.theme)
            Result.success(parsedResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun streamReflection(request: AiReflectionRequest): Flow<AiReflectionResponse> = flow {
        val prompt = StoicPromptTemplate.buildUserPrompt(request)
        var accumulatedText = ""
        generativeModel.generateContentStream(prompt).collect { chunk ->
            accumulatedText += chunk.text ?: ""
            emit(parseResponse(accumulatedText, request.theme))
        }
    }

    private fun parseResponse(json: String, theme: AiReflectionTheme): AiReflectionResponse {
        
        val cleanJson = json.removeSurrounding("```json", "```").trim()
        
        fun extractField(field: String): String {
            val regex = "\"$field\"\\s*:\\s*\"([^\"]*)\"".toRegex()
            return regex.find(cleanJson)?.groupValues?.get(1) ?: ""
        }

        return AiReflectionResponse(
            insight = extractField("insight"),
            interpretation = extractField("interpretation"),
            reflectiveQuestion = extractField("reflectiveQuestion"),
            actionableStep = extractField("actionableStep"),
            quote = extractField("quote").takeIf { it.isNotBlank() },
            principle = extractField("principle").takeIf { it.isNotBlank() },
            theme = theme
        )
    }
}
