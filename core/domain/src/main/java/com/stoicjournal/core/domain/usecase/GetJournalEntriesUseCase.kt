package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetJournalEntriesUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    operator fun invoke(filter: EntryType? = null): Flow<List<JournalEntry>> {
        return journalRepository.getAllEntries().map { entries ->
            filter?.let { entries.filter { entry -> entry.type == filter } } ?: entries
        }
    }
}
