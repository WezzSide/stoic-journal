package com.stoicjournal.feature.journal.freeform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.QuoteRepository
import com.stoicjournal.core.domain.usecase.SaveJournalEntryUseCase
import com.stoicjournal.core.domain.usecase.createJournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FreeFormJournalUiState(
    val title: String = "",
    val body: String = "",
    val attachedQuote: Quote? = null,
    val quotes: List<Quote> = emptyList()
)

@HiltViewModel
class FreeFormJournalViewModel @Inject constructor(
    quoteRepository: QuoteRepository,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FreeFormJournalUiState())
    val uiState: StateFlow<FreeFormJournalUiState> = _uiState

    init {
        quoteRepository.getAllQuotes().onEach { quotes ->
            _uiState.update { it.copy(quotes = quotes) }
        }.launchIn(viewModelScope)
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateBody(body: String) {
        _uiState.update { it.copy(body = body) }
    }

    fun attachRandomQuote() {
        val quotes = _uiState.value.quotes
        if (quotes.isNotEmpty()) {
            _uiState.update { it.copy(attachedQuote = quotes.random()) }
        }
    }

    fun removeQuote() {
        _uiState.update { it.copy(attachedQuote = null) }
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val entry = createJournalEntry(
                type = EntryType.FREE_FORM,
                body = state.body,
                title = state.title.takeIf { it.isNotBlank() },
                quoteId = state.attachedQuote?.id
            )
            saveJournalEntryUseCase(entry)
            onComplete()
        }
    }
}
