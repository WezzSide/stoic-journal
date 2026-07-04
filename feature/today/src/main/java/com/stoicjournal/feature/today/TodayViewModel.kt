package com.stoicjournal.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.Exercise
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Streak
import com.stoicjournal.core.domain.repository.ExerciseRepository
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.usecase.GetQuoteForPromptUseCase
import com.stoicjournal.core.domain.usecase.GetStreakUseCase
import com.stoicjournal.core.domain.usecase.GetTodayPromptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    getTodayPromptUseCase: GetTodayPromptUseCase,
    private val getQuoteForPromptUseCase: GetQuoteForPromptUseCase,
    getStreakUseCase: GetStreakUseCase,
    exerciseRepository: ExerciseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState

    init {
        val contentFlow = combine(
            getTodayPromptUseCase(),
            getStreakUseCase(),
            exerciseRepository.getAllExercises()
        ) { todayPrompt, streak, exercises ->
            ContentData(
                prompt = todayPrompt.prompt,
                streak = streak,
                exercises = exercises
            )
        }

        val quoteFlow = contentFlow
            .flatMapLatest { content ->
                val quoteFlow = content.prompt?.let { prompt ->
                    getQuoteForPromptUseCase(prompt)
                } ?: flowOf(null)
                quoteFlow
                    .map { quote -> content.copy(quote = quote) }
                    .onStart { emit(content) }
            }

        val permissionFlow = preferencesRepository.preferences
            .map { it.hasRequestedNotificationPermission to it.reminderEnabled }
            .distinctUntilChanged()

        combine(quoteFlow, permissionFlow) { content, (hasRequested, reminderEnabled) ->
            TodayUiState(
                prompt = content.prompt,
                quote = content.quote,
                streak = content.streak,
                exercises = content.exercises,
                hasRequestedNotificationPermission = hasRequested,
                reminderEnabled = reminderEnabled,
                isLoading = false
            )
        }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    private data class ContentData(
        val prompt: Prompt? = null,
        val quote: Quote? = null,
        val streak: Streak = Streak(0, null),
        val exercises: List<Exercise> = emptyList()
    )

    fun setNotificationPermissionRequested(requested: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationPermissionRequested(requested)
        }
    }
}
