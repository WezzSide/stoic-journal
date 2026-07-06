package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchJournalEntriesUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    operator fun invoke(query: String): Flow<List<JournalEntry>> {
        return journalRepository.getAllEntries().map { entries ->
            if (query.isBlank()) {
                entries
            } else {
                entries.filter { entry ->
                    entry.title?.contains(query, ignoreCase = true) == true ||
                    entry.body.contains(query, ignoreCase = true)
                }
            }
        }
    }
}
