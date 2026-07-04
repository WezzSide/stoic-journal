package com.stoicjournal.feature.journal.freeform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.common.di.ApplicationScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalDraft
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.JournalDraftRepository
import com.stoicjournal.core.domain.repository.QuoteRepository
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
    private val journalDraftRepository: JournalDraftRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    private val _uiState = MutableStateFlow(FreeFormJournalUiState())
    val uiState: StateFlow<FreeFormJournalUiState> = _uiState

    private var draftQuoteIdToRestore: String? = null
    private var quoteExplicitlySet: Boolean = false

    private val draftSaver = JournalDraftSaver(
        viewModelScope = viewModelScope,
        entryType = EntryType.FREE_FORM,
        key = FREE_FORM_DRAFT_KEY,
        produceDraft = {
            val state = _uiState.value
            JournalDraft(
                title = state.title,
                body = state.body,
                quoteId = state.attachedQuote?.id
            )
        },
        saveDraft = journalDraftRepository::saveDraft
    )

    init {
        viewModelScope.launch {
            val draft = journalDraftRepository.getDraft(EntryType.FREE_FORM, FREE_FORM_DRAFT_KEY).first()
            draft?.let {
                draftQuoteIdToRestore = it.quoteId
                _uiState.update { state ->
                    state.copy(
                        title = it.title,
                        body = it.body
                    )
                }
            }

            quoteRepository.getAllQuotes().onEach { quotes ->
                _uiState.update { it.copy(quotes = quotes) }
                restoreDraftQuoteIfNeeded(quotes)
            }.launchIn(viewModelScope)
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
        draftSaver.scheduleSave()
    }

    fun updateBody(body: String) {
        _uiState.update { it.copy(body = body, saveError = null) }
        draftSaver.scheduleSave()
    }

    fun attachRandomQuote() {
        val quotes = _uiState.value.quotes
        if (quotes.isNotEmpty()) {
            quoteExplicitlySet = true
            _uiState.update { it.copy(attachedQuote = quotes.random(), saveError = null) }
            draftSaver.scheduleSave()
        }
    }

    fun removeQuote() {
        quoteExplicitlySet = true
        _uiState.update { it.copy(attachedQuote = null, saveError = null) }
        draftSaver.scheduleSave()
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

    override fun onCleared() {
        draftSaver.flushOnExit(applicationScope)
        super.onCleared()
    }

    companion object {
        private const val FREE_FORM_DRAFT_KEY = "freeform"
    }
}
