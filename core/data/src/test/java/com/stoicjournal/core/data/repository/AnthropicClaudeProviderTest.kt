package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.AiError
import com.stoicjournal.core.domain.model.AiStreamEvent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** QA-05 mocked-HTTP validation of the Anthropic provider. */
class AnthropicClaudeProviderTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: AnthropicClaudeProvider

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        provider = AnthropicClaudeProvider(FakeCredentialRepository(mapOf("claude" to "sk-ant-test")), OkHttpClient())
        provider.baseUrl = server.url("/").toString().trimEnd('/')
    }

    @After fun tearDown() = server.shutdown()

    private fun sse(vararg chunks: String) = buildString {
        append("event: message_start\ndata: {\"type\":\"message_start\"}\n\n")
        chunks.forEach { append("event: content_block_delta\ndata: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"$it\"}}\n\n") }
        append("event: message_stop\ndata: {\"type\":\"message_stop\"}\n\n")
    }

    @Test fun `streaming request uses system field, current model and alternating roles`() = runBlocking {
        server.enqueue(MockResponse().setBody(sse("Breathe. ", "Then act.")))

        val events = provider.streamEvents(sampleRequest(model = "default")).toList()

        val recorded = server.takeRequest()
        assertEquals("/v1/messages", recorded.path)
        assertEquals("sk-ant-test", recorded.getHeader("x-api-key"))
        assertEquals("2023-06-01", recorded.getHeader("anthropic-version"))
        val body = Json.parseToJsonElement(recorded.body.readUtf8()).jsonObject
        assertEquals(provider.defaultModelId, body["model"]!!.jsonPrimitive.content)
        assertEquals("claude-opus-5-5", body["model"]!!.jsonPrimitive.content)
        assertEquals("SYSTEM_INSTRUCTION_MARKER", body["system"]!!.jsonPrimitive.content)
        assertEquals("true", body["stream"]!!.jsonPrimitive.content)
        val messages = body["messages"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf("user", "assistant", "user"), messages.map { it["role"]!!.jsonPrimitive.content })
        assertFalse(messages.any { it["content"]!!.jsonPrimitive.content.contains("SYSTEM_INSTRUCTION_MARKER") })

        assertEquals(listOf("Breathe. ", "Then act."), events.filterIsInstance<AiStreamEvent.TextDelta>().map { it.text })
        assertEquals("Breathe. Then act.", (events.last() as AiStreamEvent.Completed).response.content)
    }

    @Test fun `retired 3_x identifiers are no longer offered`() = runBlocking {
        val models = provider.listAvailableModels().getOrThrow()
        assertTrue(models.none { it.contains("claude-3") })
        assertTrue(models.contains("claude-opus-5-5"))
        assertTrue(models.contains("claude-sonnet-5-5"))
        assertTrue(models.contains("claude-haiku-5-5"))
    }

    @Test fun `401 and 429 are classified`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        assertEquals(AiStreamEvent.Failed(AiError.InvalidCredentials), provider.streamEvents(sampleRequest()).toList().last())
        server.enqueue(MockResponse().setResponseCode(429))
        assertEquals(AiStreamEvent.Failed(AiError.RateLimited), provider.streamEvents(sampleRequest()).toList().last())
    }

    @Test fun `generateResponse parses first text block and usage`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"content":[{"type":"text","text":"{\"title\":\"Calm\"}"}],"stop_reason":"end_turn","usage":{"input_tokens":30,"output_tokens":9}}"""
            )
        )
        val response = provider.generateResponse(sampleRequest()).getOrThrow()
        assertEquals("{\"title\":\"Calm\"}", response.content)
        assertEquals(30, response.usageMetadata.inputTokens)
        assertEquals(9, response.usageMetadata.outputTokens)
        assertEquals("end_turn", response.finishReason)
    }

    @Test fun `validateConnection pings the cheapest current model with one token`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"content":[{"type":"text","text":"."}]}"""))
        assertTrue(provider.validateConnection().getOrThrow())
        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("claude-haiku-5-5", body["model"]!!.jsonPrimitive.content)
        assertEquals("1", body["max_tokens"]!!.jsonPrimitive.content)
    }
}
