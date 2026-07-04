package com.stoicjournal.feature.journal.exercise

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.common.di.ApplicationScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.Exercise
import com.stoicjournal.core.domain.model.JournalDraft
import com.stoicjournal.core.domain.repository.ExerciseRepository
import com.stoicjournal.core.domain.repository.JournalDraftRepository
import com.stoicjournal.core.domain.usecase.SaveJournalEntryUseCase
import com.stoicjournal.core.domain.usecase.createJournalEntry
import com.stoicjournal.feature.journal.draft.JournalDraftSaver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
    val saveError: String? = null,
    val error: String? = null
)

@HiltViewModel
class ExerciseJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase,
    private val journalDraftRepository: JournalDraftRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    private val exerciseId: String = savedStateHandle["exerciseId"] ?: ""
    private val _uiState = MutableStateFlow(ExerciseJournalUiState())
    val uiState: StateFlow<ExerciseJournalUiState> = _uiState

    private val draftSaver = JournalDraftSaver(
        viewModelScope = viewModelScope,
        entryType = EntryType.EXERCISE,
        key = exerciseId,
        produceDraft = { JournalDraft(reflection = _uiState.value.reflection) },
        saveDraft = journalDraftRepository::saveDraft
    )

    init {
        viewModelScope.launch {
            if (exerciseId.isBlank()) {
                _uiState.update { it.copy(error = "Invalid exercise ID") }
            } else {
                try {
                    val exercise = exerciseRepository.getExerciseById(exerciseId)
                    if (exercise == null) {
                        _uiState.update { it.copy(error = "Exercise not found") }
                    } else {
                        _uiState.update { it.copy(exercise = exercise) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to load exercise") }
                }
            }
        }

        viewModelScope.launch {
            val draft = try {
                journalDraftRepository.getDraft(EntryType.EXERCISE, exerciseId).first()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read journal draft", e)
                null
            }
            draft?.let {
                _uiState.update { it.copy(reflection = draft.reflection) }
            }
        }
    }

    fun updateReflection(reflection: String) {
        _uiState.update { it.copy(reflection = reflection, saveError = null) }
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
                journalDraftRepository.clearDraft(EntryType.EXERCISE, exerciseId)
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
                    type = EntryType.EXERCISE,
                    body = state.reflection,
                    exerciseId = state.exercise?.id
                )
                saveJournalEntryUseCase(entry)
                try {
                    journalDraftRepository.clearDraft(EntryType.EXERCISE, exerciseId)
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
        private const val TAG = "ExerciseJournalViewModel"
    }
}
