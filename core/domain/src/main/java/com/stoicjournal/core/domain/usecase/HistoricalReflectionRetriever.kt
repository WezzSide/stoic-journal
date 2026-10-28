package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.model.MentorMemory
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.repository.MentorMemoryRepository
import com.stoicjournal.core.domain.repository.MentorMemoryPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class ContextRetrievalResult(
    val relevantEntries: List<JournalEntry>,
    val approvedMemories: List<MentorMemory>,
    val formattedContextString: String
)

@Singleton
class HistoricalReflectionRetriever @Inject constructor(
    private val journalRepository: JournalRepository,
    private val memoryRepository: MentorMemoryRepository,
    private val memoryPreferencesRepository: MentorMemoryPreferencesRepository
) {
    suspend fun retrieveContext(
        query: String,
        maxEntries: Int = 3
    ): ContextRetrievalResult {
        val prefs = memoryPreferencesRepository.preferences.first()
        if (!prefs.memoryEnabled || (!prefs.historicalJournalAccessEnabled && !prefs.useApprovedMemories)) {
            return ContextRetrievalResult(emptyList(), emptyList(), "")
        }

        val allEntries = journalRepository.getAllEntries().first()
        val keywords = query.split(" ").filter { it.length > 3 }

        val relevantEntries = if (prefs.historicalJournalAccessEnabled) {
            if (keywords.isEmpty()) {
                allEntries.take(maxEntries)
            } else {
                allEntries.filter { entry ->
                    keywords.any { kw -> entry.body.contains(kw, ignoreCase = true) || (entry.title?.contains(kw, ignoreCase = true) == true) }
                }.take(maxEntries)
            }
        } else {
            emptyList()
        }

        val approvedMemories = if (prefs.useApprovedMemories) {
            memoryRepository.getApprovedMemories().first().take(5)
        } else {
            emptyList()
        }

        val contextString = buildString {
            if (approvedMemories.isNotEmpty()) {
                append("User Preferences & Memories:\n")
                approvedMemories.forEach { mem ->
                    append("- [${mem.category}]: ${mem.content}\n")
                }
                append("\n")
            }
            if (relevantEntries.isNotEmpty()) {
                append("Relevant Past Journal Reflections:\n")
                relevantEntries.forEach { entry ->
                    append("- [${entry.createdAt}]: ${entry.title ?: "Untitled"} - ${entry.body.take(150)}...\n")
                }
            }
        }

        return ContextRetrievalResult(
            relevantEntries = relevantEntries,
            approvedMemories = approvedMemories,
            formattedContextString = contextString
        )
    }
}
