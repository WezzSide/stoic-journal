package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.AiReflectionRequest
import com.stoicjournal.core.domain.model.AiReflectionResponse
import com.stoicjournal.core.domain.model.AiReflectionTheme
import com.stoicjournal.core.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStoicReflectionUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val getRecentJournalEntriesUseCase: GetRecentJournalEntriesUseCase
) {
    suspend operator fun invoke(
        content: String,
        theme: AiReflectionTheme = AiReflectionTheme.FOCUS,
        isStreaming: Boolean = true
    ): Flow<AiReflectionResponse> {
        val recentEntries = getRecentJournalEntriesUseCase(limit = 3)
        val journalContext = if (recentEntries.isNotEmpty()) {
            recentEntries.joinToString("\n---\n") { 
                "Title: ${it.title ?: "Untitled"}\nContent: ${it.body}" 
            }
        } else {
            null
        }

        val request = AiReflectionRequest(
            content = content,
            theme = theme,
            journalContext = journalContext
        )
        
        return aiRepository.streamReflection(request)
    }
}
