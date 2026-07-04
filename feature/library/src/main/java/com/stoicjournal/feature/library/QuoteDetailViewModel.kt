package com.stoicjournal.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuoteDetailUiState(val quote: Quote? = null)

@HiltViewModel
class QuoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quoteRepository: QuoteRepository
) : ViewModel() {

    private val quoteId: String = savedStateHandle["quoteId"] ?: ""
    private val _uiState = MutableStateFlow(QuoteDetailUiState())
    val uiState: StateFlow<QuoteDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            val quote = quoteRepository.getAllQuotes().first().find { it.id == quoteId }
            _uiState.update { it.copy(quote = quote) }
        }
    }
}
