package com.stoicjournal.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.PhilosopherRepository
import com.stoicjournal.core.domain.repository.QuoteRepository
import com.stoicjournal.core.domain.usecase.ToggleFavoriteQuoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    quoteRepository: QuoteRepository,
    philosopherRepository: PhilosopherRepository,
    private val toggleFavoriteQuoteUseCase: ToggleFavoriteQuoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    val filteredQuotes: StateFlow<List<Quote>> = _uiState
        .map { state ->
            when (val filter = state.selectedFilter) {
                is LibraryFilter.All -> state.quotes
                is LibraryFilter.Favorites -> state.quotes.filter { it.isFavorite }
                is LibraryFilter.ByPhilosopher -> state.quotes.filter { it.philosopherId == filter.philosopherId }
                is LibraryFilter.ByTheme -> state.quotes.filter { it.theme == filter.theme }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        combine(
            quoteRepository.getAllQuotes(),
            philosopherRepository.getAllPhilosophers()
        ) { quotes, philosophers ->
            quotes to philosophers
        }.onEach { (quotes, philosophers) ->
            _uiState.update {
                it.copy(
                    quotes = quotes,
                    philosophers = philosophers,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun selectFilter(filter: LibraryFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun toggleFavorite(quoteId: String) {
        viewModelScope.launch {
            toggleFavoriteQuoteUseCase(quoteId)
        }
    }
}
