package com.stoicjournal.feature.ai

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.stoicjournal.core.domain.model.AiReflectionResponse
import com.stoicjournal.core.domain.model.AiReflectionTheme
import org.junit.Rule
import org.junit.Test

class AiReflectionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun idleState_showsInputAndButton() {
        composeTestRule.setContent {
            AiReflectionContent(
                uiState = AiReflectionUiState.Idle,
                onNavigateBack = {},
                onGetReflection = { _, _ -> },
                onSaveToJournal = {}
            )
        }

        composeTestRule.onNodeWithText("What is on your mind?").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Reflection input field").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Stoic Reflection").assertIsDisplayed()
    }

    @Test
    fun typingInput_enablesButton() {
        composeTestRule.setContent {
            AiReflectionContent(
                uiState = AiReflectionUiState.Idle,
                onNavigateBack = {},
                onGetReflection = { _, _ -> },
                onSaveToJournal = {}
            )
        }

        val button = composeTestRule.onNodeWithText("Get Stoic Reflection")
        button.assertDoesNotExist() // Wait, the text changes based on state, initial is "Get Stoic Reflection"
        
        composeTestRule.onNodeWithContentDescription("Reflection input field").performTextInput("I am stressed")
        composeTestRule.onNodeWithText("Get Stoic Reflection").assertIsEnabled()
    }

    @Test
    fun loadingState_showsLoadingIndicator() {
        composeTestRule.setContent {
            AiReflectionContent(
                uiState = AiReflectionUiState.Loading,
                onNavigateBack = {},
                onGetReflection = { _, _ -> },
                onSaveToJournal = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Loading indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("The philosopher is contemplating...").assertIsDisplayed()
    }

    @Test
    fun successState_showsReflectionCard() {
        val reflection = AiReflectionResponse(
            insight = "Insight text",
            interpretation = "Interpretation text",
            reflectiveQuestion = "Question text",
            actionableStep = "Step text",
            theme = AiReflectionTheme.FOCUS
        )

        composeTestRule.setContent {
            AiReflectionContent(
                uiState = AiReflectionUiState.Success(reflection),
                onNavigateBack = {},
                onGetReflection = { _, _ -> },
                onSaveToJournal = {}
            )
        }

        composeTestRule.onNodeWithText("Insight text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Interpretation text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save to Journal").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() {
        composeTestRule.setContent {
            AiReflectionContent(
                uiState = AiReflectionUiState.Error("Something went wrong"),
                onNavigateBack = {},
                onGetReflection = { _, _ -> },
                onSaveToJournal = {}
            )
        }

        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }
}
