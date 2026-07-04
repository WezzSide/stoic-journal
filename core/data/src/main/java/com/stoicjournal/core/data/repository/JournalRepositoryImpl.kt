package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.JournalEntryDao
import com.stoicjournal.core.data.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class JournalRepositoryImpl @Inject constructor(
    private val dao: JournalEntryDao
) : JournalRepository {
    override fun getAll(): Flow<List<JournalEntryEntity>> = dao.getAll()
    override suspend fun getById(id: String): JournalEntryEntity? = dao.getById(id)
    override suspend fun insert(entry: JournalEntryEntity) = dao.insert(entry)
    override suspend fun delete(entry: JournalEntryEntity) = dao.delete(entry)
}
