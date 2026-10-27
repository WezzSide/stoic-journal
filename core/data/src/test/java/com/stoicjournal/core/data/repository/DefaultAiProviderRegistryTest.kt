package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.AiProviderRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAiProviderRegistryTest {

    @Test
    fun `register and retrieve provider`() {
        val registry = DefaultAiProviderRegistry()
        val fakeProvider = FakeAiProvider()
        registry.registerProvider(fakeProvider)

        assertEquals(1, registry.getSupportedProviders().size)
        assertEquals(fakeProvider, registry.getProvider("fake_provider"))
    }

    @Test
    fun `active provider defaults to first registered`() {
        val registry = DefaultAiProviderRegistry()
        val fakeProvider = FakeAiProvider()
        registry.registerProvider(fakeProvider)

        assertEquals(fakeProvider, registry.getActiveProvider())
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
