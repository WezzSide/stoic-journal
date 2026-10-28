package com.stoicjournal.feature.ai.mentor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.ReflectionCandidate
import com.stoicjournal.core.domain.repository.ReflectionCandidateCache
import com.stoicjournal.core.domain.repository.ReflectionPersistenceCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MentorReflectionPreviewUiState(
    val candidate: ReflectionCandidate? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MentorReflectionPreviewViewModel @Inject constructor(
    private val candidateCache: ReflectionCandidateCache,
    private val persistenceCoordinator: ReflectionPersistenceCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(MentorReflectionPreviewUiState())
    val uiState: StateFlow<MentorReflectionPreviewUiState> = _uiState.asStateFlow()

    fun loadCandidate(candidateId: String) {
        val candidate = candidateCache.get(candidateId)
        if (candidate != null) {
            _uiState.update { it.copy(candidate = candidate, error = null) }
        } else {
            _uiState.update { it.copy(error = "Reflection candidate not found.") }
        }
    }

    fun saveReflection(updatedCandidate: ReflectionCandidate) {
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val result = persistenceCoordinator.saveReflectionCandidate(updatedCandidate)
            if (result.isSuccess) {
                candidateCache.remove(updatedCandidate.candidateId)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } else {
                _uiState.update { 
                    it.copy(
                        isSaving = false, 
                        error = "Failed to save: ${result.exceptionOrNull()?.message}"
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
