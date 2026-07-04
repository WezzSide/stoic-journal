package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun getAllEntries(): Flow<List<JournalEntry>>
    suspend fun getEntryById(id: String): JournalEntry?
    suspend fun saveEntry(entry: JournalEntry)
    suspend fun deleteEntry(entry: JournalEntry)
}
