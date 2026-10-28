package com.stoicjournal.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stoicjournal.core.data.repository.FakeAiProvider
import com.stoicjournal.core.domain.model.AiProviderRegistry
import com.stoicjournal.core.domain.model.AiStreamEvent
import com.stoicjournal.core.domain.repository.StoicMentorRepository
import com.stoicjournal.core.domain.usecase.MentorConversationCoordinator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/** Device-level check of the Mentor pipeline with the real Hilt graph (Room + DataStore), no UI. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MentorCoordinatorDeviceTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @Inject lateinit var coordinator: MentorConversationCoordinator
    @Inject lateinit var registry: AiProviderRegistry
    @Inject lateinit var fake: FakeAiProvider
    @Inject lateinit var mentorRepository: StoicMentorRepository
    @Inject lateinit var credentials: com.stoicjournal.core.domain.repository.SecureCredentialRepository
    private var previousSelection: String? = null

    @Before fun setUp() {
        hiltRule.inject()
        previousSelection = runBlocking { registry.observeActiveProviderId().first() }
    }

    @org.junit.After fun tearDown() {
        runBlocking { previousSelection?.let { registry.setActiveProvider(it) } }
    }

    @Test
    fun coordinator_roundTrip_withFakeProvider_completesOnDevice() = runBlocking {
        registry.setActiveProvider(FakeAiProvider.PROVIDER_ID)
        fake.generatedResponseContent = "QA device reply"
        val events = withTimeout(8_000) { coordinator.sendMessage("qa-device-conv", "QA device message").toList() }
        assertTrue(events.toString(), events.last() is AiStreamEvent.Completed)
        val msgs = mentorRepository.getMessagesForConversation("qa-device-conv").first()
        assertEquals(2, msgs.size)
        mentorRepository.deleteConversation("qa-device-conv")
    }

    @Test
    fun coordinator_withKeylessRealProvider_failsFastWithMissingCredentials() = runBlocking {
        val keyless = E2ESupport.keylessRealProviderId(credentials)
        org.junit.Assume.assumeTrue("all real providers have credentials on this device; skipping to avoid live calls", keyless != null)
        registry.setActiveProvider(keyless!!)
        try {
            val events = withTimeout(8_000) { coordinator.sendMessage("qa-device-conv-2", "QA no key").toList() }
            assertTrue(events.toString(), events.last() is AiStreamEvent.Failed)
            assertEquals(com.stoicjournal.core.domain.model.AiError.MissingCredentials, (events.last() as AiStreamEvent.Failed).error)
            val msgs = mentorRepository.getMessagesForConversation("qa-device-conv-2").first()
            assertEquals(1, msgs.size)
        } finally {
            registry.setActiveProvider(FakeAiProvider.PROVIDER_ID)
            mentorRepository.deleteConversation("qa-device-conv-2")
        }
    }
}
