package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.AiProviderRequest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ProviderVerificationTest {

    @Test
    fun `fake provider validates connection`() = runBlocking {
        val provider = FakeAiProvider()
        val result = provider.validateConnection()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `fake provider handles invalid connection`() = runBlocking {
        val provider = FakeAiProvider()
        provider.shouldFailConnection = true
        val result = provider.validateConnection()
        assertTrue(result.isFailure)
    }

    @Test
    fun `fake provider returns generated response`() = runBlocking {
        val provider = FakeAiProvider()
        val request = AiProviderRequest(
            conversationId = "1",
            userMessage = "Hello",
            selectedModel = "model",
            systemInstructionVersion = "1.0",
            authorizedHistoricalContext = "",
            conversationHistory = emptyList(),
            maxOutputTokens = 50,
            cancellationState = false,
            privacySettings = emptyMap(),
            requestCorrelationId = UUID.randomUUID().toString()
        )
        
        val response = provider.generateResponse(request)
        assertTrue(response.isSuccess)
        assertEquals("This is a fake response.", response.getOrNull()?.content)
    }

    @Test
    fun `fake provider returns streaming events`() = runBlocking {
        val provider = FakeAiProvider()
        val request = AiProviderRequest(
            conversationId = "1",
            userMessage = "Hello",
            selectedModel = "model",
            systemInstructionVersion = "1.0",
            authorizedHistoricalContext = "",
            conversationHistory = emptyList(),
            maxOutputTokens = 50,
            cancellationState = false,
            privacySettings = emptyMap(),
            requestCorrelationId = UUID.randomUUID().toString()
        )
        
        val events = provider.streamEvents(request).toList()
        assertEquals(3, events.size) // Started, TextDelta, Completed
    }
}
