package com.stoicjournal.feature.journal.freeform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalDraft
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.JournalDraftRepository
import com.stoicjournal.core.domain.repository.QuoteRepository
import com.stoicjournal.core.domain.usecase.SaveJournalEntryUseCase
import com.stoicjournal.core.domain.usecase.createJournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

data class FreeFormJournalUiState(
    val title: String = "",
    val body: String = "",
    val attachedQuote: Quote? = null,
    val quotes: List<Quote> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class FreeFormJournalViewModel @Inject constructor(
    quoteRepository: QuoteRepository,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase,
    private val journalDraftRepository: JournalDraftRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FreeFormJournalUiState())
    val uiState: StateFlow<FreeFormJournalUiState> = _uiState

    private var draftSaveJob: Job? = null
    private var draftQuoteIdToRestore: String? = null
    private var quoteExplicitlySet: Boolean = false

    init {
        quoteRepository.getAllQuotes().onEach { quotes ->
            _uiState.update { it.copy(quotes = quotes) }
            restoreDraftQuoteIfNeeded(quotes)
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            journalDraftRepository.getDraft(EntryType.FREE_FORM, FREE_FORM_DRAFT_KEY).first()?.let { draft ->
                draftQuoteIdToRestore = draft.quoteId
                _uiState.update { state ->
                    state.copy(
                        title = draft.title,
                        body = draft.body
                    )
                }
            }
        }
    }

    private fun restoreDraftQuoteIfNeeded(quotes: List<Quote>) {
        val quoteId = draftQuoteIdToRestore ?: return
        if (quoteExplicitlySet || quotes.isEmpty()) return
        val quote = quotes.find { it.id == quoteId }
        if (quote != null) {
            _uiState.update { it.copy(attachedQuote = quote) }
            draftQuoteIdToRestore = null
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, saveError = null) }
        scheduleDraftSave()
    }

    fun updateBody(body: String) {
        _uiState.update { it.copy(body = body, saveError = null) }
        scheduleDraftSave()
    }

    fun attachRandomQuote() {
        val quotes = _uiState.value.quotes
        if (quotes.isNotEmpty()) {
            quoteExplicitlySet = true
            _uiState.update { it.copy(attachedQuote = quotes.random(), saveError = null) }
            scheduleDraftSave()
        }
    }

    fun removeQuote() {
        quoteExplicitlySet = true
        _uiState.update { it.copy(attachedQuote = null, saveError = null) }
        scheduleDraftSave()
    }

    fun onSaveErrorShown() {
        _uiState.update { it.copy(saveError = null) }
    }

    fun discard(onComplete: () -> Unit) {
        viewModelScope.launch {
            journalDraftRepository.clearDraft(EntryType.FREE_FORM, FREE_FORM_DRAFT_KEY)
            onComplete()
        }
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val state = _uiState.value
                val entry = createJournalEntry(
                    type = EntryType.FREE_FORM,
                    body = state.body,
                    title = state.title.takeIf { it.isNotBlank() },
                    quoteId = state.attachedQuote?.id
                )
                saveJournalEntryUseCase(entry)
                journalDraftRepository.clearDraft(EntryType.FREE_FORM, FREE_FORM_DRAFT_KEY)
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

    private fun scheduleDraftSave() {
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(DRAFT_SAVE_DEBOUNCE_MS)
            flushDraft()
        }
    }

    override fun onCleared() {
        val pendingSave = draftSaveJob?.isCompleted == false
        draftSaveJob?.cancel()
        if (pendingSave) {
            runBlocking { flushDraft() }
        }
        super.onCleared()
    }

    private suspend fun flushDraft() {
        val state = _uiState.value
        journalDraftRepository.saveDraft(
            EntryType.FREE_FORM,
            FREE_FORM_DRAFT_KEY,
            JournalDraft(
                title = state.title,
                body = state.body,
                quoteId = state.attachedQuote?.id
            )
        )
    }

    companion object {
        private const val DRAFT_SAVE_DEBOUNCE_MS = 300L
        private const val FREE_FORM_DRAFT_KEY = "freeform"
    }
}
