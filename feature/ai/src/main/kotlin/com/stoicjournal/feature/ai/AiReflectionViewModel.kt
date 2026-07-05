package com.stoicjournal.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.usecase.GetStoicReflectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiReflectionViewModel @Inject constructor(
    private val getStoicReflectionUseCase: GetStoicReflectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiReflectionUiState>(AiReflectionUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun getReflection(content: String) {
        viewModelScope.launch {
            _uiState.value = AiReflectionUiState.Loading
            getStoicReflectionUseCase(content)
                .onSuccess { reflection ->
                    _uiState.value = AiReflectionUiState.Success(reflection)
                }
                .onFailure { error ->
                    _uiState.value = AiReflectionUiState.Error(error.message ?: "Unknown error")
                }
        }
    }
}

sealed interface AiReflectionUiState {
    data object Idle : AiReflectionUiState
    data object Loading : AiReflectionUiState
    data class Success(val reflection: String) : AiReflectionUiState
    data class Error(val message: String) : AiReflectionUiState
}
