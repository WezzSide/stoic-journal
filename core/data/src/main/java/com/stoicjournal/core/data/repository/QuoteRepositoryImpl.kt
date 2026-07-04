package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.QuoteDao
import com.stoicjournal.core.data.entity.QuoteEntity
import com.stoicjournal.core.domain.model.Theme
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class QuoteRepositoryImpl @Inject constructor(
    private val dao: QuoteDao
) : QuoteRepository {
    override fun getAll(): Flow<List<QuoteEntity>> = dao.getAll()
    override fun getByPhilosopher(philosopherId: String): Flow<List<QuoteEntity>> = dao.getByPhilosopher(philosopherId)
    override fun getByTheme(theme: Theme): Flow<List<QuoteEntity>> = dao.getByTheme(theme)
    override fun getFavorites(): Flow<List<QuoteEntity>> = dao.getFavorites()
    override suspend fun getById(id: String): QuoteEntity? = dao.getById(id)
    override suspend fun update(quote: QuoteEntity) = dao.update(quote)
}
