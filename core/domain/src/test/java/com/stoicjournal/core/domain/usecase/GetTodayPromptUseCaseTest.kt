package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.model.Theme
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.PromptRepository
import com.stoicjournal.core.domain.repository.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GetTodayPromptUseCaseTest {

    @Test
    fun `empty prompt pool returns null prompt`() = runTest {
        val promptRepository = FakePromptRepository(emptyList())
        val preferencesRepository = FakePreferencesRepository(StoicismLevel.BEGINNER)
        val useCase = GetTodayPromptUseCase(promptRepository, preferencesRepository)

        val todayPrompt = useCase().first()
        assertNull(todayPrompt.prompt)
    }

    @Test
    fun `selects deterministic prompt based on day of year`() = runTest {
        val prompts = List(5) { index ->
            Prompt(
                id = "$index",
                text = "Prompt $index",
                theme = Theme.CONTROL
            )
        }
        val promptRepository = FakePromptRepository(prompts)
        val preferencesRepository = FakePreferencesRepository(StoicismLevel.BEGINNER)
        val useCase = GetTodayPromptUseCase(promptRepository, preferencesRepository)

        val todayPrompt = useCase().first()
        val expectedIndex = LocalDate.now().dayOfYear % prompts.size
        assertEquals(prompts[expectedIndex], todayPrompt.prompt)
    }

    @Test
    fun `beginner level filters by difficulty before selecting`() = runTest {
        val beginnerPrompts = List(3) { index ->
            Prompt(
                id = "beginner-$index",
                text = "Beginner $index",
                theme = Theme.CONTROL,
                difficultyLevel = StoicismLevel.BEGINNER
            )
        }
        val advancedPrompts = List(2) { index ->
            Prompt(
                id = "advanced-$index",
                text = "Advanced $index",
                theme = Theme.CONTROL,
                difficultyLevel = StoicismLevel.ADVANCED
            )
        }
        val promptRepository = FakePromptRepository(beginnerPrompts + advancedPrompts)
        val preferencesRepository = FakePreferencesRepository(StoicismLevel.BEGINNER)
        val useCase = GetTodayPromptUseCase(promptRepository, preferencesRepository)

        val todayPrompt = useCase().first()
        val expectedIndex = LocalDate.now().dayOfYear % beginnerPrompts.size
        assertEquals(beginnerPrompts[expectedIndex], todayPrompt.prompt)
    }

    private class FakePromptRepository(
        private val prompts: List<Prompt>
    ) : PromptRepository {
        override fun getAllPrompts(): Flow<List<Prompt>> = flowOf(prompts)
        override fun getPromptsByDifficulty(level: StoicismLevel): Flow<List<Prompt>> =
            flowOf(prompts.filter { it.difficultyLevel == level })
    }

    private class FakePreferencesRepository(
        level: StoicismLevel
    ) : PreferencesRepository {
        override val preferences: Flow<UserPreferences> = MutableStateFlow(
            UserPreferences(
                reminderHour = 20,
                reminderMinute = 0,
                reminderEnabled = true,
                stoicismLevel = level,
                onboardingComplete = true
            )
        )

        override suspend fun setReminderTime(hour: Int, minute: Int) = Unit
        override suspend fun setReminderEnabled(enabled: Boolean) = Unit
        override suspend fun setStoicismLevel(level: StoicismLevel) = Unit
        override suspend fun setOnboardingComplete(complete: Boolean) = Unit
    }
}
