package com.stoicjournal.feature.journal.guided

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalDraft
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.JournalDraftRepository
import com.stoicjournal.core.domain.repository.PromptRepository
import com.stoicjournal.core.domain.usecase.GetQuoteForPromptUseCase
import com.stoicjournal.core.domain.usecase.SaveJournalEntryUseCase
import com.stoicjournal.core.domain.usecase.createJournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val isSaving: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class GuidedJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    promptRepository: PromptRepository,
    private val getQuoteForPromptUseCase: GetQuoteForPromptUseCase,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase,
    private val journalDraftRepository: JournalDraftRepository
) : ViewModel() {

    private val promptId: String = savedStateHandle["promptId"] ?: ""
    private val _uiState = MutableStateFlow(GuidedJournalUiState())
    val uiState: StateFlow<GuidedJournalUiState> = _uiState

    private var draftSaveJob: Job? = null

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

        viewModelScope.launch {
            journalDraftRepository.getDraft(EntryType.GUIDED, promptId).first()?.let { draft ->
                _uiState.update { it.copy(body = draft.body) }
            }
        }
    }

    fun updateBody(body: String) {
        _uiState.update { it.copy(body = body, saveError = null) }
        scheduleDraftSave()
    }

    fun onSaveErrorShown() {
        _uiState.update { it.copy(saveError = null) }
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val state = _uiState.value
                val entry = createJournalEntry(
                    type = EntryType.GUIDED,
                    body = state.body,
                    promptId = state.prompt?.id,
                    quoteId = state.quote?.id
                )
                saveJournalEntryUseCase(entry)
                journalDraftRepository.clearDraft(EntryType.GUIDED, promptId)
                _uiState.update { it.copy(isSaving = false) }
                onComplete()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = e.message ?: "Failed to save entry") }
            }
        }
    }

    private fun scheduleDraftSave() {
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(DRAFT_SAVE_DEBOUNCE_MS)
            val body = _uiState.value.body
            journalDraftRepository.saveDraft(
                EntryType.GUIDED,
                promptId,
                JournalDraft(body = body)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        draftSaveJob?.cancel()
    }

    companion object {
        private const val DRAFT_SAVE_DEBOUNCE_MS = 300L
    }
}
