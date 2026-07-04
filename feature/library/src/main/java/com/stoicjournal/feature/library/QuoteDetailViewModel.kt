package com.stoicjournal.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuoteDetailUiState(
    val quote: Quote? = null,
    val error: String? = null
)

@HiltViewModel
class QuoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quoteRepository: QuoteRepository
) : ViewModel() {

    private val quoteId: String = savedStateHandle["quoteId"] ?: ""
    private val _uiState = MutableStateFlow(QuoteDetailUiState())
    val uiState: StateFlow<QuoteDetailUiState> = _uiState

    init {
        if (quoteId.isBlank()) {
            _uiState.update { it.copy(error = "Invalid quote ID") }
        } else {
            viewModelScope.launch {
                try {
                    val quote = quoteRepository.getQuoteById(quoteId)
                    if (quote == null) {
                        _uiState.update { it.copy(error = "Quote not found") }
                    } else {
                        _uiState.update { it.copy(quote = quote) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message ?: "Failed to load quote") }
                }
            }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
