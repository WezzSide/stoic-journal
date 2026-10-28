package com.stoicjournal.e2e

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.stoicjournal.HiltTestActivity
import com.stoicjournal.core.domain.model.AiProviderRegistry
import com.stoicjournal.core.domain.model.MentorMemoryStatus
import com.stoicjournal.core.domain.repository.MentorMemoryPreferencesRepository
import com.stoicjournal.core.domain.repository.MentorMemoryRepository
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.ProactiveMentorPreferencesRepository
import com.stoicjournal.navigation.Screen
import com.stoicjournal.navigation.StoicJournalNavHost
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/** E2E-004 (configure/select provider), E2E-012 settings persistence, E2E-014/015/016 memory lifecycle. */
@HiltAndroidTest
class SettingsAndMemoryE2ETest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule(HiltTestActivity::class.java)

    @Inject lateinit var registry: AiProviderRegistry
    @Inject lateinit var preferences: PreferencesRepository
    @Inject lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory
    @Inject lateinit var proactivePrefs: ProactiveMentorPreferencesRepository
    @Inject lateinit var memoryPrefs: MentorMemoryPreferencesRepository
    @Inject lateinit var memoryRepository: MentorMemoryRepository
    private var previousSelection: String? = null

    @Before
    fun setUp() {
        hiltRule.inject()
        E2ESupport.ensureScreenOn()
        E2ESupport.ensureWorkManager(androidx.test.core.app.ApplicationProvider.getApplicationContext(), workerFactory)
        E2ESupport.markOnboardingComplete(preferences)
        runBlocking {
            previousSelection = registry.observeActiveProviderId().first()
            registry.setActiveProvider("openai")
            proactivePrefs.setProactiveMentorEnabled(false)
            proactivePrefs.setQuietHours(22, 8)
            proactivePrefs.setPreferredReminderTime(20, 30)
            proactivePrefs.setSnoozedUntil(null)
            proactivePrefs.setSkippedTodayDate(null)
            memoryPrefs.setMemoryEnabled(false)
            memoryPrefs.setUseApprovedMemories(false)
            memoryPrefs.setHistoricalJournalAccessEnabled(false)
            memoryPrefs.setSendHistoricalContextToProvider(false)
            memoryRepository.clearAll()
        }
        composeRule.setContent {
            val navController = TestNavHostController(LocalContext.current).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
            StoicJournalNavHost(startDestination = Screen.Settings, navController = navController)
        }
    }

    @org.junit.After
    fun tearDown() {
        runBlocking { previousSelection?.let { registry.setActiveProvider(it) } }
    }

    @Test
    fun activeProviderSwitch_requiresConfirmation_andPersists() {
        composeRule.waitForText("Actieve provider voor Stoic Mentor")
        composeRule.onNodeWithTag("ActiveProviderRow_claude").performScrollTo().performClick()
        composeRule.waitForText("Provider wisselen?")
        // Dismiss first: nothing must change without confirmation.
        composeRule.onNodeWithText("Annuleren").performClick()
        assertEquals("openai", runBlocking { registry.observeActiveProviderId().first() })

        composeRule.onNodeWithTag("ActiveProviderRow_claude").performClick()
        composeRule.waitForText("Provider wisselen?")
        composeRule.onNodeWithTag("ConfirmProviderSwitch").performClick()
        composeRule.waitForText("Gesprekken worden verstuurd naar Claude.")
        assertEquals("claude", runBlocking { registry.observeActiveProviderId().first() })
        assertEquals("claude", runBlocking { registry.getActiveProvider()!!.providerId })
    }

    @Test
    fun proactiveMentor_toggleTimeAndQuietHours_persist() {
        composeRule.waitForText("Proactieve Stoic Mentor")
        composeRule.onNodeWithTag("ProactiveMentorSwitch").performScrollTo().assertIsOff().performClick()
        composeRule.waitUntil(5_000) { runBlocking { proactivePrefs.preferences.first().proactiveMentorEnabled } }
        composeRule.onNodeWithTag("ProactiveMentorSwitch").assertIsOn()

        composeRule.onNodeWithTag("QuietStartPlus").performScrollTo().performClick()
        composeRule.onNodeWithTag("QuietEndMinus").performClick()
        composeRule.waitUntil(5_000) {
            val p = runBlocking { proactivePrefs.preferences.first() }
            p.quietHoursStartHour == 23 && p.quietHoursEndHour == 7
        }

        composeRule.onNodeWithTag("ProactiveTimeRow").performScrollTo().performClick()
        composeRule.waitForText("Reminder time")
        composeRule.onNodeWithText("OK").performClick()
        val prefs = runBlocking { proactivePrefs.preferences.first() }
        assertTrue(prefs.proactiveMentorEnabled)
        assertEquals(20, prefs.preferredReminderHour)
        assertEquals(30, prefs.preferredReminderMinute)

        composeRule.onNodeWithTag("ProactiveMentorSwitch").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { !runBlocking { proactivePrefs.preferences.first().proactiveMentorEnabled } }
    }

    @Test
    fun memoryLifecycle_enable_propose_approve_use_delete() {
        composeRule.waitForText("Geheugen & context")
        composeRule.onNodeWithTag("MemorySettingsRow").performScrollTo().performClick()
        composeRule.waitForText("Memory & Context")

        composeRule.onNodeWithTag("MemoryEnabledSwitch").assertIsOff().performClick()
        composeRule.onNodeWithTag("UseApprovedMemoriesSwitch").performClick()
        composeRule.onNodeWithTag("SendContextSwitch").performClick()
        composeRule.waitUntil(5_000) {
            val p = runBlocking { memoryPrefs.preferences.first() }
            p.memoryEnabled && p.useApprovedMemories && p.sendHistoricalContextToProvider && !p.historicalJournalAccessEnabled
        }

        composeRule.onNodeWithTag("NewMemoryField").performScrollTo().performTextInput("QA Memory 001: prefers short Socratic questions")
        composeRule.onNodeWithTag("ProposeMemoryButton").performClick()
        composeRule.waitForText("QA Memory 001", substring = true)
        val proposed = runBlocking { memoryRepository.getAllMemories().first() }.single()
        assertEquals(MentorMemoryStatus.PROPOSED, proposed.status)
        assertFalse(proposed.userApproved)
        assertTrue("unapproved memory must not be retrievable", runBlocking { memoryRepository.getApprovedMemories().first() }.isEmpty())

        composeRule.onNodeWithTag("ApproveMemory_${proposed.id}").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { runBlocking { memoryRepository.getApprovedMemories().first() }.size == 1 }
        composeRule.onNodeWithText("approved").assertIsDisplayed()

        composeRule.onNodeWithTag("DeleteMemory_${proposed.id}").performClick()
        composeRule.waitUntil(5_000) { runBlocking { memoryRepository.getAllMemories().first() }.isEmpty() }
        assertTrue(runBlocking { memoryRepository.getApprovedMemories().first() }.isEmpty())
        composeRule.onNodeWithText("No memories yet.").assertIsDisplayed()
    }
}
