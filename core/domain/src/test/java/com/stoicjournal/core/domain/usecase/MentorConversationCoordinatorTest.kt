package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.AiError
import com.stoicjournal.core.domain.model.AiProvider
import com.stoicjournal.core.domain.model.AiProviderRegistry
import com.stoicjournal.core.domain.model.AiProviderRequest
import com.stoicjournal.core.domain.model.AiProviderResponse
import com.stoicjournal.core.domain.model.AiStreamEvent
import com.stoicjournal.core.domain.model.ConversationState
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.model.MentorConversation
import com.stoicjournal.core.domain.model.MentorMemory
import com.stoicjournal.core.domain.model.MentorMessage
import com.stoicjournal.core.domain.model.MessageRole
import com.stoicjournal.core.domain.model.UsageMetadata
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.repository.MentorMemoryPreferences
import com.stoicjournal.core.domain.repository.MentorMemoryPreferencesRepository
import com.stoicjournal.core.domain.repository.MentorMemoryRepository
import com.stoicjournal.core.domain.repository.StoicMentorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Regression tests for the Stoic Mentor conversation pipeline (QA-06 / QA-09).
 * These guard the P0 defect where every provider was asked for model id "default".
 */
class MentorConversationCoordinatorTest {

    private lateinit var mentorRepository: InMemoryMentorRepository
    private lateinit var provider: RecordingProvider
    private lateinit var memoryPrefs: MutablePrefs
    private lateinit var coordinator: MentorConversationCoordinator

    @Before
    fun setUp() {
        mentorRepository = InMemoryMentorRepository()
        provider = RecordingProvider()
        memoryPrefs = MutablePrefs()
        val retriever = HistoricalReflectionRetriever(
            journalRepository = FakeJournalRepository(
                listOf(
                    JournalEntry(
                        id = "e1", type = EntryType.FREE_FORM, body = "SECRET_JOURNAL_TEXT about patience and control",
                        createdAt = Instant.now(), updatedAt = Instant.now()
                    )
                )
            ),
            memoryRepository = FakeMemoryRepository(),
            memoryPreferencesRepository = memoryPrefs
        )
        coordinator = MentorConversationCoordinator(mentorRepository, SingleProviderRegistry(provider), retriever)
    }

    @Test
    fun `new conversation uses the provider's real default model, never the literal default`() = runBlocking {
        coordinator.sendMessage("conv-1", "Today I felt impatient in traffic.").toList()

        val request = provider.lastRequest!!
        assertEquals("test-model-current", request.selectedModel)
        assertNotEquals("default", request.selectedModel)
        val saved = mentorRepository.getConversation("conv-1").value!!
        assertEquals("test-model-current", saved.modelId)
        assertEquals("recording", saved.providerId)
    }

    @Test
    fun `legacy conversation rows with modelId default are resolved to the provider default`() = runBlocking {
        mentorRepository.saveConversation(
            MentorConversation(
                id = "legacy", title = "old", createdAt = Instant.now(), updatedAt = Instant.now(),
                providerId = "recording", modelId = "default", state = ConversationState.ACTIVE,
                reflectionStatus = null, linkedJournalEntryId = null
            )
        )

        coordinator.sendMessage("legacy", "Continue our talk about control.").toList()

        assertEquals("test-model-current", provider.lastRequest!!.selectedModel)
    }

    @Test
    fun `user message is persisted exactly once and excluded from the history sent to the provider`() = runBlocking {
        coordinator.sendMessage("conv-2", "First message about courage today.").toList()
        coordinator.sendMessage("conv-2", "Second message about temperance.").toList()

        val messages = mentorRepository.getMessagesForConversation("conv-2").value
        val userMessages = messages.filter { it.role == MessageRole.USER }
        assertEquals(2, userMessages.size)
        assertEquals(1, userMessages.count { it.content == "Second message about temperance." })

        val secondRequest = provider.lastRequest!!
        assertEquals("Second message about temperance.", secondRequest.userMessage)
        assertFalse(secondRequest.conversationHistory.any { it.content == "Second message about temperance." })
        assertEquals(listOf(MessageRole.USER, MessageRole.MENTOR), secondRequest.conversationHistory.map { it.role })
        assertEquals(listOf(1, 2, 3, 4), messages.map { it.sequenceNumber })
    }

    @Test
    fun `system instruction is carried separately from the user content`() = runBlocking {
        coordinator.sendMessage("conv-3", "Hello mentor").toList()

        val request = provider.lastRequest!!
        assertTrue(request.authorizedHistoricalContext!!.contains("You are Stoic Mentor"))
        assertFalse(request.userMessage.contains("You are Stoic Mentor"))
    }

    @Test
    fun `completed stream persists the mentor reply and emits Started, deltas and Completed`() = runBlocking {
        provider.replyChunks = listOf("Consider ", "what is ", "in your control.")

        val events = coordinator.sendMessage("conv-4", "I worry about tomorrow's review.").toList()

        assertTrue(events.first() is AiStreamEvent.Started)
        assertEquals(3, events.count { it is AiStreamEvent.TextDelta })
        assertTrue(events.last() is AiStreamEvent.Completed)
        val mentor = mentorRepository.getMessagesForConversation("conv-4").value.single { it.role == MessageRole.MENTOR }
        assertEquals("Consider what is in your control.", mentor.content)
    }

    @Test
    fun `provider failure leaves the user message but writes no mentor message`() = runBlocking {
        provider.failWith = AiError.InvalidCredentials

        val events = coordinator.sendMessage("conv-5", "Will this fail?").toList()

        assertTrue(events.any { it is AiStreamEvent.Failed && it.error == AiError.InvalidCredentials })
        val messages = mentorRepository.getMessagesForConversation("conv-5").value
        assertEquals(1, messages.size)
        assertEquals(MessageRole.USER, messages.single().role)
    }

    @Test
    fun `journal context is never sent unless every consent flag including send-to-provider is on`() = runBlocking {
        memoryPrefs.state.value = MentorMemoryPreferences(
            memoryEnabled = true, historicalJournalAccessEnabled = true, useApprovedMemories = true,
            sendHistoricalContextToProvider = false
        )
        coordinator.sendMessage("conv-6", "I need patience and control today").toList()
        assertFalse(provider.lastRequest!!.authorizedHistoricalContext!!.contains("SECRET_JOURNAL_TEXT"))

        memoryPrefs.state.value = memoryPrefs.state.value.copy(sendHistoricalContextToProvider = true)
        coordinator.sendMessage("conv-6", "I need patience and control today").toList()
        val context = provider.lastRequest!!.authorizedHistoricalContext!!
        assertTrue(context.contains("SECRET_JOURNAL_TEXT"))
        assertTrue("context must be delimited as untrusted data", context.contains(HistoricalReflectionRetriever.CONTEXT_BEGIN))
    }

    @Test
    fun `no active provider yields ProviderUnavailable without touching persistence`() = runBlocking {
        val emptyCoordinator = MentorConversationCoordinator(
            mentorRepository,
            object : AiProviderRegistry {
                override fun registerProvider(provider: AiProvider) {}
                override fun getSupportedProviders(): List<AiProvider> = emptyList()
                override fun getProvider(providerId: String): AiProvider? = null
                override suspend fun getActiveProvider(): AiProvider? = null
                override suspend fun setActiveProvider(providerId: String) {}
                override fun observeActiveProviderId(): Flow<String?> = flowOf(null)
            },
            HistoricalReflectionRetriever(FakeJournalRepository(emptyList()), FakeMemoryRepository(), memoryPrefs)
        )
        val events = emptyCoordinator.sendMessage("conv-7", "hello").toList()
        assertEquals(listOf<AiStreamEvent>(AiStreamEvent.Failed(AiError.ProviderUnavailable)), events)
        assertNull(mentorRepository.getConversation("conv-7").value)
    }

    // ---- fakes ----

    private class RecordingProvider : AiProvider {
        override val providerId = "recording"
        override val defaultModelId = "test-model-current"
        var lastRequest: AiProviderRequest? = null
        var replyChunks = listOf("ok")
        var failWith: AiError? = null

        override suspend fun validateConnection() = Result.success(true)
        override suspend fun listAvailableModels() = Result.success(listOf(defaultModelId))
        override suspend fun generateResponse(request: AiProviderRequest): Result<AiProviderResponse> {
            lastRequest = request
            return Result.success(response(request, replyChunks.joinToString("")))
        }
        override fun streamResponse(request: AiProviderRequest): Flow<Result<AiProviderResponse>> = flow { emit(generateResponse(request)) }
        override fun streamEvents(request: AiProviderRequest): Flow<AiStreamEvent> = flow {
            lastRequest = request
            emit(AiStreamEvent.Started(request.requestCorrelationId))
            failWith?.let { emit(AiStreamEvent.Failed(it)); return@flow }
            replyChunks.forEach { emit(AiStreamEvent.TextDelta(it)) }
            emit(AiStreamEvent.Completed(response(request, replyChunks.joinToString(""))))
        }
        override fun cancelRequest(requestId: String) {}
        override fun reportUsage() = UsageMetadata(0, 0, 0, null)
        private fun response(request: AiProviderRequest, text: String) = AiProviderResponse(
            content = text, provider = providerId, model = request.selectedModel, completionStatus = "completed",
            usageMetadata = UsageMetadata(1, 1, 0, null), finishReason = "stop", errorClassification = null
        )
    }

    private class SingleProviderRegistry(private val provider: AiProvider) : AiProviderRegistry {
        override fun registerProvider(provider: AiProvider) {}
        override fun getSupportedProviders() = listOf(provider)
        override fun getProvider(providerId: String) = provider.takeIf { it.providerId == providerId }
        override suspend fun getActiveProvider(): AiProvider = provider
        override suspend fun setActiveProvider(providerId: String) {}
        override fun observeActiveProviderId(): Flow<String?> = flowOf(provider.providerId)
    }

    class InMemoryMentorRepository : StoicMentorRepository {
        private val conversations = MutableStateFlow<Map<String, MentorConversation>>(emptyMap())
        private val messages = MutableStateFlow<List<MentorMessage>>(emptyList())
        override suspend fun saveConversation(conversation: MentorConversation) {
            conversations.value = conversations.value + (conversation.id to conversation)
        }
        override fun getConversation(conversationId: String): MutableStateFlow<MentorConversation?> =
            MutableStateFlow(conversations.value[conversationId])
        override fun getAllConversations(): Flow<List<MentorConversation>> = conversations.map { it.values.toList() }
        override suspend fun deleteConversation(conversationId: String) {
            conversations.value = conversations.value - conversationId
        }
        override suspend fun saveMessage(message: MentorMessage) {
            messages.value = messages.value.filterNot { it.id == message.id } + message
        }
        override fun getMessagesForConversation(conversationId: String): MutableStateFlow<List<MentorMessage>> =
            MutableStateFlow(messages.value.filter { it.conversationId == conversationId }.sortedBy { it.sequenceNumber })
        override suspend fun clearAll() { conversations.value = emptyMap(); messages.value = emptyList() }
    }

    private class FakeJournalRepository(private val entries: List<JournalEntry>) : JournalRepository {
        override fun getAllEntries(): Flow<List<JournalEntry>> = flowOf(entries)
        override suspend fun getEntryById(id: String) = entries.find { it.id == id }
        override suspend fun saveEntry(entry: JournalEntry) {}
        override suspend fun deleteEntry(entry: JournalEntry) {}
    }

    private class FakeMemoryRepository : MentorMemoryRepository {
        override fun getApprovedMemories(): Flow<List<MentorMemory>> = flowOf(emptyList())
        override fun getAllMemories(): Flow<List<MentorMemory>> = flowOf(emptyList())
        override suspend fun saveMemory(memory: MentorMemory) {}
        override suspend fun approveMemory(id: String) {}
        override suspend fun deleteMemory(id: String) {}
        override suspend fun clearAll() {}
    }

    private class MutablePrefs : MentorMemoryPreferencesRepository {
        val state = MutableStateFlow(MentorMemoryPreferences())
        override val preferences: Flow<MentorMemoryPreferences> = state
        override suspend fun setMemoryEnabled(enabled: Boolean) { state.value = state.value.copy(memoryEnabled = enabled) }
        override suspend fun setHistoricalJournalAccessEnabled(enabled: Boolean) { state.value = state.value.copy(historicalJournalAccessEnabled = enabled) }
        override suspend fun setUsePreviousConversations(enabled: Boolean) { state.value = state.value.copy(usePreviousConversations = enabled) }
        override suspend fun setUseApprovedMemories(enabled: Boolean) { state.value = state.value.copy(useApprovedMemories = enabled) }
        override suspend fun setSendHistoricalContextToProvider(enabled: Boolean) { state.value = state.value.copy(sendHistoricalContextToProvider = enabled) }
    }
}
