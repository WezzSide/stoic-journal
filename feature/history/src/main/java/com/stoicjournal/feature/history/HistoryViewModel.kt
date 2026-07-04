package com.stoicjournal.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.usecase.GetJournalEntriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getJournalEntriesUseCase: GetJournalEntriesUseCase,
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<EntryType?>(null)
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    init {
        selectedFilter
            .flatMapLatest { filter ->
                getJournalEntriesUseCase(filter)
                    .map { Result.success(it) }
                    .catch { e -> emit(Result.failure(e)) }
            }
            .onEach { result ->
                _uiState.update { state ->
                    state.copy(
                        entries = result.getOrNull() ?: emptyList(),
                        isLoading = false,
                        loadError = result.exceptionOrNull()?.localizedMessage
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun selectFilter(filter: EntryType?) {
        selectedFilter.value = filter
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            try {
                journalRepository.deleteEntry(entry)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(deleteError = e.localizedMessage ?: "Failed to delete entry")
                }
            }
        }
    }

    fun deleteErrorShown() {
        _uiState.update { it.copy(deleteError = null) }
    }
}
