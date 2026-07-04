package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Theme
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    fun getAllQuotes(): Flow<List<Quote>>
    fun getQuotesByPhilosopher(philosopherId: String): Flow<List<Quote>>
    fun getQuotesByTheme(theme: Theme): Flow<List<Quote>>
    fun getFavoriteQuotes(): Flow<List<Quote>>
    suspend fun toggleFavorite(quoteId: String)
}
