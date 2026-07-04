package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.QuoteDao
import com.stoicjournal.core.data.entity.QuoteEntity
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Theme
import com.stoicjournal.core.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuoteRepositoryImpl @Inject constructor(
    private val quoteDao: QuoteDao
) : QuoteRepository {

    override fun getAllQuotes(): Flow<List<Quote>> = quoteDao.getAll().map { it.map(QuoteEntity::toDomain) }
    override fun getQuotesByPhilosopher(philosopherId: String): Flow<List<Quote>> =
        quoteDao.getByPhilosopher(philosopherId).map { it.map(QuoteEntity::toDomain) }

    override fun getQuotesByTheme(theme: Theme): Flow<List<Quote>> =
        quoteDao.getByTheme(theme).map { it.map(QuoteEntity::toDomain) }

    override fun getFavoriteQuotes(): Flow<List<Quote>> =
        quoteDao.getFavorites().map { it.map(QuoteEntity::toDomain) }

    override suspend fun getQuoteById(id: String): Quote? =
        quoteDao.getById(id)?.toDomain()

    override suspend fun toggleFavorite(quoteId: String) {
        val quote = quoteDao.getById(quoteId) ?: return
        quoteDao.update(quote.copy(isFavorite = !quote.isFavorite))
    }
}

private fun QuoteEntity.toDomain() = Quote(
    id = id,
    text = text,
    attribution = attribution,
    philosopherId = philosopherId,
    theme = theme,
    source = source,
    isFavorite = isFavorite
)
