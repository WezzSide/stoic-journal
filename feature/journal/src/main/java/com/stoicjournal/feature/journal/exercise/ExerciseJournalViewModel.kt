package com.stoicjournal.feature.journal.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.Exercise
import com.stoicjournal.core.domain.model.JournalDraft
import com.stoicjournal.core.domain.repository.ExerciseRepository
import com.stoicjournal.core.domain.repository.JournalDraftRepository
import com.stoicjournal.core.domain.usecase.SaveJournalEntryUseCase
import com.stoicjournal.core.domain.usecase.createJournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseJournalUiState(
    val exercise: Exercise? = null,
    val reflection: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class ExerciseJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase,
    private val journalDraftRepository: JournalDraftRepository
) : ViewModel() {

    private val exerciseId: String = savedStateHandle["exerciseId"] ?: ""
    private val _uiState = MutableStateFlow(ExerciseJournalUiState())
    val uiState: StateFlow<ExerciseJournalUiState> = _uiState

    private var draftSaveJob: Job? = null

    init {
        viewModelScope.launch {
            val exercise = exerciseRepository.getAllExercises().first().find { it.id == exerciseId }
            _uiState.update { it.copy(exercise = exercise) }
        }

        viewModelScope.launch {
            journalDraftRepository.getDraft(EntryType.EXERCISE, exerciseId).first()?.let { draft ->
                _uiState.update { it.copy(reflection = draft.reflection) }
            }
        }
    }

    fun updateReflection(reflection: String) {
        _uiState.update { it.copy(reflection = reflection, saveError = null) }
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
                    type = EntryType.EXERCISE,
                    body = state.reflection,
                    exerciseId = state.exercise?.id
                )
                saveJournalEntryUseCase(entry)
                journalDraftRepository.clearDraft(EntryType.EXERCISE, exerciseId)
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
            val reflection = _uiState.value.reflection
            journalDraftRepository.saveDraft(
                EntryType.EXERCISE,
                exerciseId,
                JournalDraft(reflection = reflection)
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
