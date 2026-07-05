package com.stoicjournal.core.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.stoicjournal.core.data.BuildConfig
import com.stoicjournal.core.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiAiRepositoryImpl @Inject constructor() : AiRepository {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    override suspend fun generateReflection(prompt: String, context: String?): Result<String> {
        return try {
            val fullPrompt = if (context != null) "$context\n\n$prompt" else prompt
            val response = generativeModel.generateContent(fullPrompt)
            Result.success(response.text ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun streamReflection(prompt: String, context: String?): Flow<String> {
        val fullPrompt = if (context != null) "$context\n\n$prompt" else prompt
        return generativeModel.generateContentStream(fullPrompt)
            .map { it.text ?: "" }
    }
}
