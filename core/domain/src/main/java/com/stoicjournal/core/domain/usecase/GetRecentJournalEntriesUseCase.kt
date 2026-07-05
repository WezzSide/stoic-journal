package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetRecentJournalEntriesUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    suspend operator fun invoke(limit: Int = 5): List<JournalEntry> {
        return journalRepository.getAllEntries()
            .first()
            .sortedByDescending { it.createdAt }
            .take(limit)
    }
}
