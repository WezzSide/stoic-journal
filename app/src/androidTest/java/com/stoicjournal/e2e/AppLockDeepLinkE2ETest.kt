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
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        runBlocking {
            preferences.setPinLockEnabled(false)
            preferences.clearPinCode()
        }
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
