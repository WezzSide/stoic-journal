package com.stoicjournal.e2e

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.stoicjournal.MainActivity
import com.stoicjournal.core.common.notifications.NotificationRoutes
import com.stoicjournal.core.domain.repository.PreferencesRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * E2E-018: a proactive-mentor notification deep link must not bypass the PIN lock, and after the
 * correct PIN the requested Mentor destination is shown.
 */
@HiltAndroidTest
class AppLockDeepLinkE2ETest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createEmptyComposeRule()

    @Inject lateinit var preferences: PreferencesRepository
    @Inject lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        hiltRule.inject()
        E2ESupport.ensureScreenOn()
        E2ESupport.ensureWorkManager(androidx.test.core.app.ApplicationProvider.getApplicationContext(), workerFactory)
        runBlocking {
            preferences.setOnboardingComplete(true)
            preferences.setBiometricLockEnabled(false)
            preferences.setPinCode("4321")
            preferences.setPinLockEnabled(true)
            preferences.setLockTimeoutSeconds(0)
            preferences.resetPinFailures()
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        runBlocking {
            preferences.setPinLockEnabled(false)
            preferences.clearPinCode()
            preferences.resetPinFailures()
        }
    }

    /**
     * D-24 regression: after five wrong PINs the keypad is throttled — even the correct PIN is refused —
     * and the throttle survives an activity/process relaunch (it is persisted, not in-memory only).
     */
    @Test
    fun repeatedWrongPins_throttleEntry_evenForCorrectPin_andSurviveRelaunch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitForText("Bewaak je innerlijke rust")

        repeat(5) {
            listOf("1", "1", "1", "1").forEach { composeRule.onNodeWithText(it).performClick() }
        }
        composeRule.waitForText("Te veel onjuiste pogingen", substring = true)

        // Correct PIN is ignored while throttled: still on the lock screen.
        listOf("4", "3", "2", "1").forEach { composeRule.onNodeWithText(it).performClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Bewaak je innerlijke rust").assertIsDisplayed()

        val persisted = runBlocking { preferences.preferences.first() }
        assertEquals(5, persisted.pinFailedAttempts)
        assertTrue(persisted.pinLockoutUntilMillis > System.currentTimeMillis())

        // Relaunch: the throttle message is shown again immediately.
        scenario?.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitForText("Te veel onjuiste pogingen", substring = true)

        // Expire the throttle (simulated clock: rewrite the deadline) and unlock with the correct PIN.
        runBlocking { preferences.resetPinFailures() }
        scenario?.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitForText("Bewaak je innerlijke rust")
        listOf("4", "3", "2", "1").forEach { composeRule.onNodeWithText(it).performClick() }
        composeRule.waitForTextGone("Bewaak je innerlijke rust")
    }

    @Test
    fun notificationDeepLink_isGatedByPinLock_thenNavigatesToMentor() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            putExtra(NotificationRoutes.EXTRA_ROUTE, NotificationRoutes.ROUTE_MENTOR_CHAT)
        }
        scenario = ActivityScenario.launch(intent)

        composeRule.waitForText("Bewaak je innerlijke rust")
        assertTrue(composeRule.onAllNodesWithText("Stoic Mentor").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Conversations").fetchSemanticsNodes().isEmpty())

        // wrong PIN first
        listOf("1", "1", "1", "1").forEach { composeRule.onNodeWithText(it).performClick() }
        composeRule.waitForText("Onjuiste PIN-code, probeer opnieuw.")

        listOf("4", "3", "2", "1").forEach { composeRule.onNodeWithText(it).performClick() }
        composeRule.waitForText("Stoic Mentor")
        composeRule.onNodeWithText("Stoic Mentor").assertIsDisplayed()
    }
}
