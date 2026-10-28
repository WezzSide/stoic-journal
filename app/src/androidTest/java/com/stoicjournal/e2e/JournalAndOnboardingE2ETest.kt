package com.stoicjournal.e2e

import android.Manifest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.rule.GrantPermissionRule
import com.stoicjournal.HiltTestActivity
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.navigation.Screen
import com.stoicjournal.navigation.StoicJournalNavHost
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/** E2E-001 (first launch / onboarding) and E2E-002/003 (create, reopen a journal entry). */
@HiltAndroidTest
class JournalAndOnboardingE2ETest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    @get:Rule(order = 2) val composeRule = createAndroidComposeRule(HiltTestActivity::class.java)

    @Inject lateinit var journalRepository: JournalRepository
    @Inject lateinit var preferences: PreferencesRepository
    @Inject lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    @Before
    fun setUp() {
        hiltRule.inject()
        E2ESupport.ensureScreenOn()
        E2ESupport.ensureWorkManager(androidx.test.core.app.ApplicationProvider.getApplicationContext(), workerFactory)
    }

    private fun setNavHost(start: Screen) {
        composeRule.setContent {
            val navController = TestNavHostController(LocalContext.current).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
            StoicJournalNavHost(startDestination = start, navController = navController)
        }
    }

    @Test
    fun onboarding_firstLaunch_completesAndPersists() {
        runBlocking { preferences.setOnboardingComplete(false); preferences.setPinLockEnabled(false) }
        setNavHost(Screen.Onboarding)

        composeRule.waitForText("Begin")
        composeRule.onNodeWithText("Begin").performClick()
        composeRule.waitForText("Continue")
        composeRule.onNodeWithText("Familiar").performClick()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.waitForText("Start journaling")
        composeRule.onNodeWithText("Start journaling").performClick()
        composeRule.waitForText("Stoic Journal")

        val prefs = runBlocking { preferences.preferences.first() }
        assertTrue(prefs.onboardingComplete)
        assertEquals("FAMILIAR", prefs.stoicismLevel.name)
    }

    @Test
    fun freeFormEntry_isSaved_reopenedFromHistory() {
        E2ESupport.markOnboardingComplete(preferences)
        setNavHost(Screen.Today)
        val body = "QA Journal Entry 001 — I noticed my mind racing and returned to the present moment."

        composeRule.waitForText("Free-form")
        composeRule.onNodeWithText("Free-form").performClick()
        composeRule.waitForText("Free-form Entry")
        composeRule.onNode(hasText("Title (optional)")).performTextInput("QA Journal Entry 001")
        composeRule.onNode(hasText("Write freely...")).performTextInput(body)
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForText("Stoic Journal")

        val saved = runBlocking { journalRepository.getAllEntries().first() }.filter { it.title == "QA Journal Entry 001" }
        assertEquals(1, saved.size)
        assertEquals(EntryType.FREE_FORM, saved.single().type)
        assertEquals(body, saved.single().body)

        composeRule.onNodeWithContentDescription("History").performClick()
        composeRule.waitForText("QA Journal Entry 001")
        composeRule.onNodeWithText("QA Journal Entry 001").performClick()
        composeRule.waitForText("Entry")
        composeRule.onNodeWithText(body, substring = true).assertIsDisplayed()
    }
}
