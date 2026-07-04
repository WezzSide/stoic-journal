package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.PromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

class GetTodayPromptUseCase @Inject constructor(
    private val promptRepository: PromptRepository,
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<TodayPrompt> = combine(
        promptRepository.getAllPrompts(),
        preferencesRepository.preferences
    ) { prompts, prefs ->
        val today = LocalDate.now()
        val candidates = if (prefs.stoicismLevel == StoicismLevel.BEGINNER) {
            prompts.filter { it.difficultyLevel == StoicismLevel.BEGINNER }
        } else {
            prompts
        }
        val pool = candidates.ifEmpty { prompts }
        val selected = pool.firstOrNull()
        TodayPrompt(
            prompt = selected,
            date = today
        )
    }
}

data class TodayPrompt(
    val prompt: Prompt?,
    val date: LocalDate
)
