package com.stoicjournal.feature.journal.guided

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.common.di.ApplicationScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalDraft
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.JournalDraftRepository
import com.stoicjournal.core.domain.repository.PromptRepository
import com.stoicjournal.core.domain.usecase.GetQuoteForPromptUseCase
import com.stoicjournal.core.domain.usecase.SaveJournalEntryUseCase
import com.stoicjournal.core.domain.usecase.createJournalEntry
import com.stoicjournal.feature.journal.draft.JournalDraftSaver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
    val saveError: String? = null,
    val error: String? = null
)

@HiltViewModel
class GuidedJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    promptRepository: PromptRepository,
    private val getQuoteForPromptUseCase: GetQuoteForPromptUseCase,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase,
    private val journalDraftRepository: JournalDraftRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    private val promptId: String = savedStateHandle["promptId"] ?: ""
    private val _uiState = MutableStateFlow(GuidedJournalUiState())
    val uiState: StateFlow<GuidedJournalUiState> = _uiState

    private val draftSaver = JournalDraftSaver(
        viewModelScope = viewModelScope,
        entryType = EntryType.GUIDED,
        key = promptId,
        produceDraft = { JournalDraft(body = _uiState.value.body) },
        saveDraft = journalDraftRepository::saveDraft
    )

    init {
        viewModelScope.launch {
            if (promptId.isBlank()) {
                _uiState.update { it.copy(error = "Invalid prompt ID") }
            } else {
                val prompt = promptRepository.getAllPrompts().first().find { it.id == promptId }
                if (prompt == null) {
                    _uiState.update { it.copy(error = "Prompt not found") }
                } else {
                    _uiState.update { it.copy(prompt = prompt) }
                    getQuoteForPromptUseCase(prompt).onEach { quote ->
                        _uiState.update { it.copy(quote = quote) }
                    }.launchIn(viewModelScope)
                }
            }
        }

        viewModelScope.launch {
            val draft = try {
                journalDraftRepository.getDraft(EntryType.GUIDED, promptId).first()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read journal draft", e)
                null
            }
            draft?.let {
                _uiState.update { it.copy(body = draft.body) }
            }
        }
    }

    fun updateBody(body: String) {
        _uiState.update { it.copy(body = body, saveError = null) }
        draftSaver.scheduleSave()
    }

    fun onSaveErrorShown() {
        _uiState.update { it.copy(saveError = null) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    fun discard(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                journalDraftRepository.clearDraft(EntryType.GUIDED, promptId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear journal draft", e)
            }
            draftSaver.disableAutoSave()
            onComplete()
        }
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
                try {
                    journalDraftRepository.clearDraft(EntryType.GUIDED, promptId)
                } catch (e: CancellationException) {
                    _uiState.update { it.copy(isSaving = false) }
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to clear journal draft", e)
                }
                draftSaver.disableAutoSave()
                _uiState.update { it.copy(isSaving = false) }
                onComplete()
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isSaving = false) }
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = e.message ?: "Failed to save entry") }
            }
        }
    }

    override fun onCleared() {
        draftSaver.flushOnExit(applicationScope)
        super.onCleared()
    }

    companion object {
        private const val TAG = "GuidedJournalViewModel"
    }
}
