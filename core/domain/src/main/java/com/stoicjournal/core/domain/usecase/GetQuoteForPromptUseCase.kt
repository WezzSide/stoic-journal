package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetQuoteForPromptUseCase @Inject constructor(
    private val quoteRepository: QuoteRepository
) {
    operator fun invoke(prompt: Prompt): Flow<Quote?> =
        quoteRepository.getAllQuotes().map { quotes ->
            val matching = quotes.filter { it.theme == prompt.theme || it.philosopherId == prompt.philosopherId }
            matching.firstOrNull() ?: quotes.firstOrNull()
        }
}
