package com.stoicjournal.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.usecase.GetJournalEntriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class EntryDetailUiState(val entry: JournalEntry? = null)

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getJournalEntriesUseCase: GetJournalEntriesUseCase
) : ViewModel() {

    private val entryId: String = savedStateHandle["entryId"] ?: ""
    private val _uiState = MutableStateFlow(EntryDetailUiState())
    val uiState: StateFlow<EntryDetailUiState> = _uiState

    init {
        getJournalEntriesUseCase().onEach { entries ->
            _uiState.update { it.copy(entry = entries.find { entry -> entry.id == entryId }) }
        }.launchIn(viewModelScope)
    }
}
