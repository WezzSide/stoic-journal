package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class SaveJournalEntryUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    suspend operator fun invoke(entry: JournalEntry) {
        journalRepository.saveEntry(entry)
    }
}

fun createJournalEntry(
    type: com.stoicjournal.core.domain.model.EntryType,
    body: String,
    promptId: String? = null,
    title: String? = null,
    quoteId: String? = null,
    exerciseId: String? = null
): JournalEntry {
    val now = Instant.now()
    return JournalEntry(
        id = UUID.randomUUID().toString(),
        type = type,
        promptId = promptId,
        title = title,
        body = body,
        quoteId = quoteId,
        exerciseId = exerciseId,
        createdAt = now,
        updatedAt = now
    )
}
