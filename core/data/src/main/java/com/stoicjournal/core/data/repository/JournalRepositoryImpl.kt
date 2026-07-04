package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.JournalEntryDao
import com.stoicjournal.core.data.entity.JournalEntryEntity
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class JournalRepositoryImpl @Inject constructor(
    private val journalEntryDao: JournalEntryDao
) : JournalRepository {

    override fun getAllEntries(): Flow<List<JournalEntry>> {
        return journalEntryDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEntryById(id: String): JournalEntry? {
        return journalEntryDao.getById(id)?.toDomain()
    }

    override suspend fun saveEntry(entry: JournalEntry) {
        journalEntryDao.insert(entry.toEntity())
    }

    override suspend fun deleteEntry(entry: JournalEntry) {
        journalEntryDao.delete(entry.toEntity())
    }
}

private fun JournalEntryEntity.toDomain() = JournalEntry(
    id = id,
    type = type,
    promptId = promptId,
    title = title,
    body = body,
    quoteId = quoteId,
    exerciseId = exerciseId,
    createdAt = Instant.ofEpochMilli(createdAtMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtMillis)
)

private fun JournalEntry.toEntity() = JournalEntryEntity(
    id = id,
    type = type,
    promptId = promptId,
    title = title,
    body = body,
    quoteId = quoteId,
    exerciseId = exerciseId,
    createdAtMillis = createdAt.toEpochMilli(),
    updatedAtMillis = updatedAt.toEpochMilli()
)
