package com.stoicjournal.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.Exercise
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Streak
import com.stoicjournal.core.domain.repository.ExerciseRepository
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.usecase.GetQuoteForPromptUseCase
import com.stoicjournal.core.domain.usecase.GetStreakUseCase
import com.stoicjournal.core.domain.usecase.GetTodayPromptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    getTodayPromptUseCase: GetTodayPromptUseCase,
    private val getQuoteForPromptUseCase: GetQuoteForPromptUseCase,
    getStreakUseCase: GetStreakUseCase,
    exerciseRepository: ExerciseRepository,
    journalRepository: JournalRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState

    init {
        val entriesFlow = journalRepository.getAllEntries()
            .catch { emit(emptyList()) }

        val contentFlow = combine(
            getTodayPromptUseCase(),
            getStreakUseCase(),
            exerciseRepository.getAllExercises(),
            entriesFlow
        ) { todayPrompt, streak, exercises, entries ->
            val today = LocalDate.now()
            val pastEntry = entries.find { entry ->
                val entryDate = entry.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
                entryDate.isBefore(today) && (
                    (entryDate.dayOfMonth == today.dayOfMonth && entryDate.month == today.month) ||
                    entryDate == today.minusDays(7) ||
                    entryDate == today.minusDays(30)
                )
            }
            ContentData(
                prompt = todayPrompt.prompt,
                streak = streak,
                exercises = exercises,
                onThisDayEntry = pastEntry
            )
        }
            .catch { e ->
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadError = e.localizedMessage ?: "Failed to load today's content"
                    )
                }
                emit(ContentData())
            }

        val quoteFlow = contentFlow
            .map { it.prompt }
            .distinctUntilChanged()
            .flatMapLatest { prompt ->
                (prompt?.let { getQuoteForPromptUseCase(it) } ?: flowOf(null))
                    .onStart { emit(null) }
                    .catch { e ->
                        if (e is CancellationException) throw e
                        _uiState.update {
                            it.copy(
                                loadError = e.localizedMessage ?: "Failed to load quote"
                            )
                        }
                        emit(null)
                    }
            }

        val permissionFlow = preferencesRepository.preferences
            .map { it.hasRequestedNotificationPermission to it.reminderEnabled }
            .distinctUntilChanged()

        combine(contentFlow, quoteFlow, permissionFlow) { content, quote, (hasRequested, reminderEnabled) ->
            TodayUiState(
                prompt = content.prompt,
                quote = quote,
                streak = content.streak,
                exercises = content.exercises,
                onThisDayEntry = content.onThisDayEntry,
                hasRequestedNotificationPermission = hasRequested,
                reminderEnabled = reminderEnabled,
                isLoading = false,
                loadError = _uiState.value.loadError
            )
        }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    private data class ContentData(
        val prompt: Prompt? = null,
        val quote: Quote? = null,
        val streak: Streak = Streak(0, null),
        val exercises: List<Exercise> = emptyList(),
        val onThisDayEntry: JournalEntry? = null
    )

    fun setNotificationPermissionRequested(requested: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationPermissionRequested(requested)
        }
    }

    fun loadErrorShown() {
        _uiState.update { it.copy(loadError = null) }
    }
}
