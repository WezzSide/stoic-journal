package com.stoicjournal.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.AiReflectionResponse
import com.stoicjournal.core.domain.model.AiReflectionTheme
import com.stoicjournal.core.domain.usecase.GetStoicReflectionUseCase
import com.stoicjournal.core.domain.usecase.SaveAiReflectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiReflectionViewModel @Inject constructor(
    private val getStoicReflectionUseCase: GetStoicReflectionUseCase,
    private val saveAiReflectionUseCase: SaveAiReflectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiReflectionUiState>(AiReflectionUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var currentThought: String = ""

    fun getReflection(content: String, theme: AiReflectionTheme) {
        if (content.isBlank()) {
            _uiState.value = AiReflectionUiState.Error("Please enter your thoughts.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AiReflectionUiState.Loading
            currentThought = content
            
            getStoicReflectionUseCase(content, theme)
                .catch { error ->
                    _uiState.value = AiReflectionUiState.Error(error.message ?: "Failed to get reflection")
                }
                .collect { reflection ->
                    _uiState.value = AiReflectionUiState.Success(reflection, isSaved = false)
                }
        }
    }

    fun saveToJournal() {
        val state = uiState.value
        if (state is AiReflectionUiState.Success && !state.isSaved) {
            viewModelScope.launch {
                saveAiReflectionUseCase(currentThought, state.reflection)
                    .onSuccess {
                        _uiState.value = state.copy(isSaved = true)
                    }
                    .onFailure { error ->
                        _uiState.value = AiReflectionUiState.Error("Failed to save entry: ${error.message}")
                    }
            }
        }
    }

    fun clearError() {
        if (_uiState.value is AiReflectionUiState.Error) {
            _uiState.value = AiReflectionUiState.Idle
        }
    }
}

sealed interface AiReflectionUiState {
    data object Idle : AiReflectionUiState
    data object Loading : AiReflectionUiState
    data class Success(
        val reflection: AiReflectionResponse,
        val isSaved: Boolean = false
    ) : AiReflectionUiState
    data class Error(val message: String) : AiReflectionUiState
}
