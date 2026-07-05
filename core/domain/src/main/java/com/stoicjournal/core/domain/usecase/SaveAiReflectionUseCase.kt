package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.AiReflectionResponse
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import java.util.UUID
import javax.inject.Inject

class SaveAiReflectionUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    suspend operator fun invoke(
        userThought: String,
        reflection: AiReflectionResponse
    ): Result<Unit> {
        val entry = JournalEntry(
            id = UUID.randomUUID().toString(),
            type = EntryType.GUIDED, // Mapping AI reflection to a guided entry type for now
            title = "AI Reflection: ${reflection.theme.displayName}",
            body = """
                My thought: $userThought
                
                Stoic Insight: ${reflection.insight}
                
                Interpretation: ${reflection.interpretation}
                
                Reflective Question: ${reflection.reflectiveQuestion}
                
                Actionable Step: ${reflection.actionableStep}
                
                ${reflection.quote?.let { "Quote: $it" } ?: ""}
                ${reflection.principle?.let { "Principle: $it" } ?: ""}
            """.trimIndent(),
            createdAtMillis = System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis()
        )
        
        return try {
            journalRepository.insertEntry(entry)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
