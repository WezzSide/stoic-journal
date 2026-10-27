package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.AiReflectionRequest
import com.stoicjournal.core.domain.model.AiReflectionResponse
import com.stoicjournal.core.domain.model.AiReflectionTheme
import com.stoicjournal.core.domain.repository.AiRepository
import com.stoicjournal.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetWeeklySummaryUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(): Result<AiReflectionResponse> {
        val lastWeek = Instant.now().minus(7, ChronoUnit.DAYS)
        val recentEntries = journalRepository.getAllEntries().first()
            .filter { it.createdAt.isAfter(lastWeek) }
        
        if (recentEntries.isEmpty()) {
            return Result.failure(Exception("No entries found in the last week."))
        }

        val context = recentEntries.joinToString("\n---\n") { 
            "Date: ${it.createdAt}\nTitle: ${it.title ?: "Untitled"}\nContent: ${it.body}" 
        }

        val request = AiReflectionRequest(
            content = "Please summarize my Stoic journey over the last week based on these entries.",
            theme = AiReflectionTheme.FOCUS,
            journalContext = context
        )
        
        return aiRepository.getReflection(request)
    }
}
