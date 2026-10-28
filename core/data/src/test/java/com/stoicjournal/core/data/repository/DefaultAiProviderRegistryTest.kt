package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.AiProviderRequest
import com.stoicjournal.core.domain.model.ProviderCredential
import com.stoicjournal.core.domain.repository.SecureCredentialRepository
import com.stoicjournal.core.domain.repository.AiProviderSelectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAiProviderRegistryTest {

    private val fakeRepo = object : SecureCredentialRepository {
        override suspend fun saveCredential(providerId: String, credential: ProviderCredential) {}
        override suspend fun getCredential(providerId: String): Result<ProviderCredential> = Result.failure(Exception())
        override suspend fun deleteCredential(providerId: String) {}
        override suspend fun hasCredential(providerId: String): Boolean = false
    }
    private val client = OkHttpClient()

    private val openAi = OpenAiProvider(fakeRepo, client)
    private val claude = AnthropicClaudeProvider(fakeRepo, client)
    private val gemini = GeminiAiProvider(fakeRepo)
    private val fake = FakeAiProvider()

    private class InMemorySelection : AiProviderSelectionRepository {
        val state = MutableStateFlow<String?>(null)
        override val activeProviderId: Flow<String?> = state
        override suspend fun setActiveProviderId(providerId: String) { state.value = providerId }
    }
    private val selection = InMemorySelection()
    private fun registry() = DefaultAiProviderRegistry(openAi, claude, gemini, fake, selection)

    @Test
    fun `register and retrieve provider`() {
        val registry = registry()

        // The fake provider is resolvable by id for tests but hidden from the user-facing list.
        assertEquals(listOf("openai", "claude", "gemini"), registry.getSupportedProviders().map { it.providerId })
        assertEquals(fake, registry.getProvider("fake_provider"))
        assertEquals(openAi, registry.getProvider("openai"))
    }

    @Test
    fun `active provider falls back to OpenAI until the user chooses`() = runBlocking {
        val registry = registry()
        assertEquals(openAi, registry.getActiveProvider())
        assertEquals(null, registry.observeActiveProviderId().first())
    }

    @Test
    fun `active provider follows the persisted selection and is observable`() = runBlocking {
        val registry = registry()
        registry.setActiveProvider("claude")
        assertEquals(claude, registry.getActiveProvider())
        assertEquals("claude", registry.observeActiveProviderId().first())
        registry.setActiveProvider("gemini")
        assertEquals(gemini, registry.getActiveProvider())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `selecting an unknown provider is rejected`() = runBlocking {
        registry().setActiveProvider("not-a-provider")
    }

    @Test
    fun `every real provider resolves the legacy placeholder to a current model`() {
        listOf(openAi, claude, gemini).forEach { provider ->
            assertEquals(provider.defaultModelId, provider.resolveModelId("default"))
            assertEquals(provider.defaultModelId, provider.resolveModelId(""))
            assertEquals(provider.defaultModelId, provider.resolveModelId(null))
            assertEquals("explicit-model", provider.resolveModelId("explicit-model"))
            assertTrue(provider.defaultModelId.isNotBlank() && provider.defaultModelId != "default")
        }
        assertEquals("gemini-3.8-flash", gemini.defaultModelId)
        assertTrue(runBlocking { gemini.listAvailableModels().getOrThrow() }.none { it.startsWith("gemini-1.5") || it == "gemini-2.0-flash" })
    }

    @Test
    fun `generate response returns expected content`() = runBlocking {
        val fakeProvider = FakeAiProvider()
        fakeProvider.generatedResponseContent = "Success output"
        val request = AiProviderRequest(
            conversationId = "1",
            userMessage = "Hello",
            selectedModel = "fake-model-1",
            systemInstructionVersion = "1.0",
            authorizedHistoricalContext = null,
            conversationHistory = emptyList(),
            maxOutputTokens = 100,
            cancellationState = false,
            privacySettings = emptyMap(),
            requestCorrelationId = "req1"
        )
        val response = fakeProvider.generateResponse(request)
        assertTrue(response.isSuccess)
        assertEquals("Success output", response.getOrThrow().content)
    }
}
