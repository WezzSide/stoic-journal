package com.stoicjournal.feature.today

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.stoicjournal.core.domain.model.Exercise
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Streak
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.model.Theme
import org.junit.Rule
import org.junit.Test

class TodayScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun todayScreen_displaysExpectedContent() {
        val prompt = Prompt(
            id = "prompt-1",
            text = "What is in your control today?",
            theme = Theme.CONTROL,
            difficultyLevel = StoicismLevel.BEGINNER
        )
        val quote = Quote(
            id = "quote-1",
            text = "You have power over your mind.",
            attribution = "Marcus Aurelius",
            philosopherId = "marcus",
            theme = Theme.CONTROL
        )
        val exercise = Exercise(
            id = "exercise-1",
            title = "Morning Reflection",
            description = "Plan your day with virtue.",
            steps = listOf("Step 1", "Step 2"),
            theme = Theme.WISDOM
        )
        val uiState = TodayUiState(
            prompt = prompt,
            quote = quote,
            streak = Streak(currentCount = 5, lastEntryDate = null),
            exercises = listOf(exercise),
            isLoading = false
        )

        composeTestRule.setContent {
            TodayScreenContent(
                uiState = uiState,
                onGuidedJournal = {},
                onFreeFormJournal = {},
                onExercise = {},
                onNavigateToLibrary = {},
                onNavigateToHistory = {},
                onNavigateToSettings = {}
            )
        }

        composeTestRule.onNodeWithText("Stoic Journal").assertExists()
        composeTestRule.onNodeWithTag("StreakIndicator").assertExists()
        composeTestRule.onNodeWithText("\"You have power over your mind.\"").assertExists()
        composeTestRule.onNodeWithText("What is in your control today?").assertExists()
        composeTestRule.onNodeWithText("Stoic Exercises").assertExists()
        composeTestRule.onNodeWithText("Morning Reflection").assertExists()
    }
}
