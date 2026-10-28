package com.stoicjournal.e2e

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasText
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso
import com.stoicjournal.HiltTestActivity
import com.stoicjournal.core.data.repository.FakeAiProvider
import com.stoicjournal.core.domain.model.AiProviderRegistry
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.MessageRole
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.StoicMentorRepository
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

/**
 * E2E-005..E2E-010: Today → Mentor → new conversation → send → streamed reply → leave → resume →
 * generate reflection → edit → save → exactly one journal entry → visible in History.
 * Uses the offline FakeAiProvider (explicitly selected) so no paid/live inference happens.
 */
@HiltAndroidTest
class MentorJourneyE2ETest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule(HiltTestActivity::class.java)

    @Inject lateinit var registry: AiProviderRegistry
    @Inject lateinit var fakeProvider: FakeAiProvider
    @Inject lateinit var mentorRepository: StoicMentorRepository
    @Inject lateinit var journalRepository: JournalRepository
    @Inject lateinit var preferences: PreferencesRepository
    @Inject lateinit var credentials: com.stoicjournal.core.domain.repository.SecureCredentialRepository
    private var previousSelection: String? = null
    @Inject lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    private lateinit var navController: TestNavHostController
    private val userText = "QA Conversation 001: today I felt impatient but kept my temper under control."

    @Before
    fun setUp() {
        hiltRule.inject()
        E2ESupport.ensureScreenOn()
        E2ESupport.ensureWorkManager(androidx.test.core.app.ApplicationProvider.getApplicationContext(), workerFactory)
        E2ESupport.markOnboardingComplete(preferences)
        runBlocking {
            previousSelection = registry.observeActiveProviderId().first()
            mentorRepository.clearAll()
            registry.setActiveProvider(FakeAiProvider.PROVIDER_ID)
        }
        fakeProvider.generatedResponseContent = "QA mentor reply: what here was within your control?"
        fakeProvider.shouldFailConnection = false
        composeRule.waitForIdle()
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
            StoicJournalNavHost(startDestination = Screen.Today, navController = navController)
        }
    }

    @org.junit.After
    fun tearDown() {
        // Restore whatever provider the device owner had selected before the test touched it.
        runBlocking { previousSelection?.let { registry.setActiveProvider(it) } }
    }

    @Test
    fun fullMentorJourney_chat_resume_reflect_saveOnce_appearsInHistory() {
        // E2E-005 open mentor
        composeRule.waitForText("AI Mentor")
        composeRule.onNodeWithText("AI Mentor").performClick()
        composeRule.waitForText("Conversations")
        composeRule.onNodeWithContentDescription("New Conversation").performClick()
        composeRule.waitForText("Stoic Mentor")
        composeRule.onNodeWithText("Provider: FAKE_PROVIDER").assertIsDisplayed()

        // E2E-006 send + streamed reply
        composeRule.onNode(hasSetTextAction()).performTextInput(userText)
        composeRule.onNodeWithContentDescription("Send Message").performClick()
        composeRule.waitForText("QA mentor reply", substring = true)
        composeRule.onAllNodesWithText(userText).assertCountEquals(1)

        val request = fakeProvider.lastRequest!!
        assertEquals("fake-model-1", request.selectedModel)
        assertEquals(userText, request.userMessage)
        assertTrue(request.authorizedHistoricalContext!!.contains("You are Stoic Mentor"))

        // follow-up
        composeRule.onNode(hasSetTextAction()).performTextInput("QA follow-up: how do I prepare for tomorrow?")
        composeRule.onNodeWithContentDescription("Send Message").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("QA mentor reply", substring = true).fetchSemanticsNodes().size == 2 }
        assertEquals(listOf(MessageRole.USER, MessageRole.MENTOR), fakeProvider.lastRequest!!.conversationHistory.map { it.role })

        val conversation = runBlocking { mentorRepository.getAllConversations().first() }.single()
        val saved = runBlocking { mentorRepository.getMessagesForConversation(conversation.id).first() }
        assertEquals(listOf(1, 2, 3, 4), saved.map { it.sequenceNumber })
        assertEquals(FakeAiProvider.PROVIDER_ID, conversation.providerId)
        assertEquals("fake-model-1", conversation.modelId)

        // E2E-007 leave and resume from the list
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForText("Conversations")
        composeRule.onNodeWithText(userText.take(30)).assertIsDisplayed()
        composeRule.onNodeWithText(userText.take(30)).performClick()
        composeRule.waitForText(userText)
        composeRule.onAllNodesWithText("QA mentor reply", substring = true).assertCountEquals(2)

        // E2E-008 structured reflection
        fakeProvider.generatedResponseContent = """{"title":"QA Reflection 001","summary":"Kept my temper in traffic.","whatHappened":"Traffic jam","keyInsights":"Anger is a choice","withinControl":"My reaction","outsideControl":"The traffic","nextAction":"Breathe before reacting"}"""
        composeRule.onNodeWithText("Generate Reflection").performClick()
        composeRule.waitForText("Review Reflection")
        composeRule.onNodeWithText("Kept my temper in traffic.").assertIsDisplayed()

        // edit title then save (E2E-009)
        composeRule.onNodeWithText("QA Reflection 001").performTextClearance()
        composeRule.onNode(hasText("Title")).performTextInput("QA Reflection 001 edited")
        composeRule.onNodeWithText("Save to Journal").performClick()
        composeRule.waitForText("Stoic Journal")

        val entries = runBlocking { journalRepository.getAllEntries().first() }.filter { it.title?.startsWith("QA Reflection 001") == true }
        assertEquals("exactly one journal entry for one candidate", 1, entries.size)
        assertEquals(EntryType.GUIDED, entries.single().type)
        assertEquals("QA Reflection 001 edited", entries.single().title)
        assertTrue(entries.single().body.contains("Breathe before reacting"))

        // E2E-010 visible in history and openable
        composeRule.onNodeWithContentDescription("History").performClick()
        composeRule.waitForText("QA Reflection 001 edited")
        composeRule.onNodeWithText("QA Reflection 001 edited").performClick()
        composeRule.waitForText("Entry")
        composeRule.onNodeWithText("Breathe before reacting", substring = true).assertIsDisplayed()
    }

    @Test
    fun providerFailure_isRecoverable_andLeavesNoMentorMessage() {
        fakeProvider.shouldFailConnection = false
        composeRule.waitForText("AI Mentor")
        composeRule.onNodeWithText("AI Mentor").performClick()
        composeRule.waitForText("Conversations")
        composeRule.onNodeWithContentDescription("New Conversation").performClick()
        composeRule.waitForText("Stoic Mentor")

        // Switch to a real provider that has NO stored credential: must fail gracefully with MissingCredentials.
        // (Skipped if the owner configured keys for all three providers — we never make live calls.)
        val keyless = E2ESupport.keylessRealProviderId(credentials)
        org.junit.Assume.assumeTrue("all real providers have credentials on this device; skipping to avoid live calls", keyless != null)
        runBlocking { registry.setActiveProvider(keyless!!) }
        composeRule.onNode(hasSetTextAction()).performTextInput("QA error path")
        composeRule.onNodeWithContentDescription("Send Message").performClick()
        composeRule.waitForText("Reflection failed", substring = true)
        composeRule.onNodeWithText("Dismiss").performClick()
        composeRule.waitForTextGone("Dismiss")

        val conversation = runBlocking { mentorRepository.getAllConversations().first() }.single()
        val saved = runBlocking { mentorRepository.getMessagesForConversation(conversation.id).first() }
        assertEquals(1, saved.size)
        assertEquals(MessageRole.USER, saved.single().role)

        // Recovery: select the working provider again and continue the same conversation.
        runBlocking { registry.setActiveProvider(FakeAiProvider.PROVIDER_ID) }
        composeRule.onNode(hasSetTextAction()).performTextInput("QA retry")
        composeRule.onNodeWithContentDescription("Send Message").performClick()
        composeRule.waitForText("QA mentor reply", substring = true)
        val after = runBlocking { mentorRepository.getMessagesForConversation(conversation.id).first() }
        assertEquals(3, after.size)
    }
}
