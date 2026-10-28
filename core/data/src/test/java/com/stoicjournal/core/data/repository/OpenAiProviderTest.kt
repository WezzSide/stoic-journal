package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.AiError
import com.stoicjournal.core.domain.model.AiStreamEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/** QA-05 mocked-HTTP validation of the OpenAI provider (request shape, SSE parsing, error mapping). */
class OpenAiProviderTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: OpenAiProvider
    private val client = OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build()

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        provider = OpenAiProvider(FakeCredentialRepository(mapOf("openai" to "sk-test-key")), client)
        provider.baseUrl = server.url("/").toString().trimEnd('/')
    }

    @After fun tearDown() = server.shutdown()

    private fun sse(vararg chunks: String): String = buildString {
        chunks.forEach { c ->
            append("data: {\"choices\":[{\"delta\":{\"content\":${Json.encodeToString(kotlinx.serialization.serializer<String>(), c)}}}]}\n\n")
        }
        append("data: [DONE]\n\n")
    }

    @Test fun `streaming request has correct body, headers and role order and yields deltas`() = runBlocking {
        server.enqueue(MockResponse().setBody(sse("Consider ", "control.")).setHeader("Content-Type", "text/event-stream"))

        val events = provider.streamEvents(sampleRequest(model = "default")).toList()

        val recorded = server.takeRequest()
        assertEquals("/v1/chat/completions", recorded.path)
        assertEquals("Bearer sk-test-key", recorded.getHeader("Authorization"))
        val body = Json.parseToJsonElement(recorded.body.readUtf8()).jsonObject
        assertEquals(provider.defaultModelId, body["model"]!!.jsonPrimitive.content)
        assertEquals("true", body["stream"]!!.jsonPrimitive.content)
        assertEquals("false", body["store"]!!.jsonPrimitive.content)
        val messages = body["messages"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf("system", "user", "assistant", "user"), messages.map { it["role"]!!.jsonPrimitive.content })
        assertEquals("SYSTEM_INSTRUCTION_MARKER", messages[0]["content"]!!.jsonPrimitive.content)
        assertEquals("Today I lost my temper in a meeting.", messages.last()["content"]!!.jsonPrimitive.content)
        assertEquals(1, messages.count { it["content"]!!.jsonPrimitive.content == "Today I lost my temper in a meeting." })

        assertTrue(events.first() is AiStreamEvent.Started)
        assertEquals(listOf("Consider ", "control."), events.filterIsInstance<AiStreamEvent.TextDelta>().map { it.text })
        val completed = events.last() as AiStreamEvent.Completed
        assertEquals("Consider control.", completed.response.content)
        assertEquals(provider.defaultModelId, completed.response.model)
        assertEquals(0, provider.activeCallCount())
    }

    @Test fun `explicit model id is passed through unchanged`() = runBlocking {
        server.enqueue(MockResponse().setBody(sse("x")))
        provider.streamEvents(sampleRequest(model = "gpt-6-astra")).toList()
        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("gpt-6-astra", body["model"]!!.jsonPrimitive.content)
    }

    @Test fun `401 maps to InvalidCredentials`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{\"error\":{\"message\":\"bad key\"}}"))
        val events = provider.streamEvents(sampleRequest()).toList()
        assertEquals(AiStreamEvent.Failed(AiError.InvalidCredentials), events.last())
    }

    @Test fun `429 maps to RateLimited`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429))
        assertEquals(AiStreamEvent.Failed(AiError.RateLimited), provider.streamEvents(sampleRequest()).toList().last())
    }

    @Test fun `404 unknown model maps to ProviderUnavailable`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        assertEquals(AiStreamEvent.Failed(AiError.ProviderUnavailable), provider.streamEvents(sampleRequest()).toList().last())
    }

    @Test fun `malformed SSE lines are skipped without aborting the stream`() = runBlocking {
        val body = "data: {not json}\n\n" + sse("ok")
        server.enqueue(MockResponse().setBody(body))
        val events = provider.streamEvents(sampleRequest()).toList()
        assertEquals(listOf("ok"), events.filterIsInstance<AiStreamEvent.TextDelta>().map { it.text })
        assertTrue(events.last() is AiStreamEvent.Completed)
    }

    @Test fun `missing credential fails before any network call`() = runBlocking {
        val noKey = OpenAiProvider(FakeCredentialRepository(emptyMap()), client).also { it.baseUrl = server.url("/").toString().trimEnd('/') }
        val events = noKey.streamEvents(sampleRequest()).toList()
        assertEquals(listOf<AiStreamEvent>(AiStreamEvent.Failed(AiError.MissingCredentials)), events)
        assertEquals(0, server.requestCount)
    }

    @Test fun `network timeout surfaces as a recoverable Failed event`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val events = withTimeout(10_000) { provider.streamEvents(sampleRequest()).toList() }
        assertTrue(events.last() is AiStreamEvent.Failed)
        assertTrue((events.last() as AiStreamEvent.Failed).error is AiError.Unknown)
    }

    @Test fun `cancelling the collector cancels the HTTP call`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(sse("a", "b", "c", "d", "e", "f")).throttleBody(8, 500, TimeUnit.MILLISECONDS)
        )
        val job = CoroutineScope(Dispatchers.IO).launch {
            provider.streamEvents(sampleRequest(correlationId = "cancel-me")).collect { }
        }
        delay(400)
        assertEquals(1, provider.activeCallCount())
        job.cancelAndJoin()
        assertEquals(0, provider.activeCallCount())
    }

    @Test fun `generateResponse parses content and usage`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"choices":[{"message":{"role":"assistant","content":"{\"title\":\"T\"}"},"finish_reason":"stop"}],"usage":{"prompt_tokens":12,"completion_tokens":7}}"""
            )
        )
        val result = provider.generateResponse(sampleRequest())
        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals("{\"title\":\"T\"}", response.content)
        assertEquals(12, response.usageMetadata.inputTokens)
        assertEquals(7, response.usageMetadata.outputTokens)
        assertEquals(provider.defaultModelId, response.model)
        assertFalse(server.takeRequest().body.readUtf8().contains("\"stream\":true"))
    }
}
