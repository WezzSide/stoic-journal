package com.stoicjournal.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.repository.ExerciseRepository
import com.stoicjournal.core.domain.usecase.GetQuoteForPromptUseCase
import com.stoicjournal.core.domain.usecase.GetStreakUseCase
import com.stoicjournal.core.domain.usecase.GetTodayPromptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    getTodayPromptUseCase: GetTodayPromptUseCase,
    private val getQuoteForPromptUseCase: GetQuoteForPromptUseCase,
    getStreakUseCase: GetStreakUseCase,
    exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState

    init {
        combine(
            getTodayPromptUseCase(),
            getStreakUseCase(),
            exerciseRepository.getAllExercises()
        ) { todayPrompt, streak, exercises ->
            Triple(todayPrompt, streak, exercises)
        }.onEach { (todayPrompt, streak, exercises) ->
            _uiState.update {
                it.copy(
                    prompt = todayPrompt.prompt,
                    streak = streak,
                    exercises = exercises,
                    isLoading = false
                )
            }
            getQuoteForPromptUseCase(todayPrompt.prompt).onEach { quote ->
                _uiState.update { it.copy(quote = quote) }
            }.launchIn(viewModelScope)
        }.launchIn(viewModelScope)
    }
}
