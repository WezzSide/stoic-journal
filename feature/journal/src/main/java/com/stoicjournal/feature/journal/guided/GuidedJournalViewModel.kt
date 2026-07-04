package com.stoicjournal.feature.journal.guided

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.PromptRepository
import com.stoicjournal.core.domain.usecase.GetQuoteForPromptUseCase
import com.stoicjournal.core.domain.usecase.SaveJournalEntryUseCase
import com.stoicjournal.core.domain.usecase.createJournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuidedJournalUiState(
    val prompt: Prompt? = null,
    val quote: Quote? = null,
    val body: String = "",
    val isSaving: Boolean = false
)

@HiltViewModel
class GuidedJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    promptRepository: PromptRepository,
    private val getQuoteForPromptUseCase: GetQuoteForPromptUseCase,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase
) : ViewModel() {

    private val promptId: String = savedStateHandle["promptId"] ?: ""
    private val _uiState = MutableStateFlow(GuidedJournalUiState())
    val uiState: StateFlow<GuidedJournalUiState> = _uiState

    init {
        viewModelScope.launch {
            val prompt = promptRepository.getAllPrompts().first().find { it.id == promptId }
            _uiState.update { it.copy(prompt = prompt) }
            prompt?.let { p ->
                getQuoteForPromptUseCase(p).onEach { quote ->
                    _uiState.update { it.copy(quote = quote) }
                }.launchIn(viewModelScope)
            }
        }
    }

    fun updateBody(body: String) {
        _uiState.update { it.copy(body = body) }
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val entry = createJournalEntry(
                type = EntryType.GUIDED,
                body = state.body,
                promptId = state.prompt?.id,
                quoteId = state.quote?.id
            )
            saveJournalEntryUseCase(entry)
            onComplete()
        }
    }
}
