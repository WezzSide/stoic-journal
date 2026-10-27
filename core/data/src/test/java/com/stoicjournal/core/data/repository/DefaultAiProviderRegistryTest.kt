package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.AiProviderRequest
import com.stoicjournal.core.domain.model.ProviderCredential
import com.stoicjournal.core.domain.repository.SecureCredentialRepository
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

    @Test
    fun `register and retrieve provider`() {
        val registry = DefaultAiProviderRegistry(openAi, claude, gemini, fake)

        assertEquals(4, registry.getSupportedProviders().size)
        assertEquals(fake, registry.getProvider("fake_provider"))
        assertEquals(openAi, registry.getProvider("openai"))
    }

    @Test
    fun `active provider defaults to first registered`() {
        val registry = DefaultAiProviderRegistry(openAi, claude, gemini, fake)

        assertEquals(openAi, registry.getActiveProvider())
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
