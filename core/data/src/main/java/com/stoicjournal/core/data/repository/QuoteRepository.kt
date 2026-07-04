package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.entity.QuoteEntity
import com.stoicjournal.core.domain.model.Theme
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    fun getAll(): Flow<List<QuoteEntity>>
    fun getByPhilosopher(philosopherId: String): Flow<List<QuoteEntity>>
    fun getByTheme(theme: Theme): Flow<List<QuoteEntity>>
    fun getFavorites(): Flow<List<QuoteEntity>>
    suspend fun getById(id: String): QuoteEntity?
    suspend fun update(quote: QuoteEntity)
}
