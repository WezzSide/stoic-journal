package com.stoicjournal.feature.journal.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.Exercise
import com.stoicjournal.core.domain.repository.ExerciseRepository
import com.stoicjournal.core.domain.usecase.SaveJournalEntryUseCase
import com.stoicjournal.core.domain.usecase.createJournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseJournalUiState(
    val exercise: Exercise? = null,
    val reflection: String = ""
)

@HiltViewModel
class ExerciseJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase
) : ViewModel() {

    private val exerciseId: String = savedStateHandle["exerciseId"] ?: ""
    private val _uiState = MutableStateFlow(ExerciseJournalUiState())
    val uiState: StateFlow<ExerciseJournalUiState> = _uiState

    init {
        viewModelScope.launch {
            val exercise = exerciseRepository.getAllExercises().first().find { it.id == exerciseId }
            _uiState.update { it.copy(exercise = exercise) }
        }
    }

    fun updateReflection(reflection: String) {
        _uiState.update { it.copy(reflection = reflection) }
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val entry = createJournalEntry(
                type = EntryType.EXERCISE,
                body = state.reflection,
                exerciseId = state.exercise?.id
            )
            saveJournalEntryUseCase(entry)
            onComplete()
        }
    }
}
