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

    @Before fun setUp() { hiltRule.inject() }

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
}
