package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.repository.AiRepository
import javax.inject.Inject

class GetStoicReflectionUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(entryContent: String): Result<String> {
        val systemPrompt = """
            You are a Stoic philosopher and guide. 
            Analyze the following journal entry and provide a brief, insightful Stoic reflection. 
            Refer to principles from Marcus Aurelius, Epictetus, or Seneca where appropriate.
            Keep the tone encouraging, calm, and objective.
        """.trimIndent()
        
        return aiRepository.generateReflection(
            prompt = entryContent,
            context = systemPrompt
        )
    }
}
