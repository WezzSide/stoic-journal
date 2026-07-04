package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.repository.QuoteRepository
import javax.inject.Inject

class ToggleFavoriteQuoteUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository
) {
    suspend operator fun invoke(quoteId: String) {
        quoteRepository.toggleFavorite(quoteId)
    }
}
