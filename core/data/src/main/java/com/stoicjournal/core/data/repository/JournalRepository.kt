package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun getAll(): Flow<List<JournalEntryEntity>>
    suspend fun getById(id: String): JournalEntryEntity?
    suspend fun insert(entry: JournalEntryEntity)
    suspend fun delete(entry: JournalEntryEntity)
}
