package com.stoicjournal.feature.ai.mentor

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.stoicjournal.core.domain.model.AiError
import com.stoicjournal.core.domain.model.MentorMessage
import com.stoicjournal.core.domain.model.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale

class MentorChatLogicTest {

    private val zone = ZoneOffset.UTC
    private fun msg(seq: Int, role: MessageRole, day: Int, hour: Int = 10) = MentorMessage(
        id = "m$seq", conversationId = "c", role = role, content = "QA $seq",
        timestamp = ZonedDateTime.of(2026, 10, day, hour, 0, 0, 0, zone).toInstant(), sequenceNumber = seq
    )

    // ---- buildChatItems ---------------------------------------------------------------------

    @Test
    fun `day header precedes the first message of every day`() {
        val items = buildChatItems(
            listOf(msg(1, MessageRole.USER, 27), msg(2, MessageRole.MENTOR, 27), msg(3, MessageRole.USER, 28)),
            isStreaming = false, streamingText = "", zone = zone
        )
        assertEquals(
            listOf("day-2026-10-27", "m1", "m2", "day-2026-10-28", "m3"),
            items.map { it.key }
        )
    }

    @Test
    fun `consecutive messages from the same sender are grouped`() {
        val items = buildChatItems(
            listOf(msg(1, MessageRole.USER, 28), msg(2, MessageRole.USER, 28), msg(3, MessageRole.MENTOR, 28)),
            isStreaming = false, streamingText = "", zone = zone
        ).filterIsInstance<ChatItem.Message>()
        assertEquals(listOf(true, false, true), items.map { it.isFirstInGroup })
        assertEquals(listOf(false, true, true), items.map { it.isLastInGroup })
    }

    @Test
    fun `system messages are hidden and order follows sequence number`() {
        val items = buildChatItems(
            listOf(msg(3, MessageRole.MENTOR, 28), msg(1, MessageRole.SYSTEM, 28), msg(2, MessageRole.USER, 28)),
            isStreaming = false, streamingText = "", zone = zone
        )
        assertEquals(listOf("day-2026-10-28", "m2", "m3"), items.map { it.key })
    }

    @Test
    fun `typing indicator before first token, streaming bubble afterwards`() {
        val base = listOf(msg(1, MessageRole.USER, 28))
        assertEquals(ChatItem.Typing, buildChatItems(base, isStreaming = true, streamingText = "", zone = zone).last())
        assertEquals(ChatItem.Streaming("Wh"), buildChatItems(base, isStreaming = true, streamingText = "Wh", zone = zone).last())
        assertTrue(buildChatItems(base, isStreaming = false, streamingText = "", zone = zone).last() is ChatItem.Message)
    }

    @Test
    fun `day labels`() {
        val today = LocalDate.of(2026, 10, 28)
        assertEquals("Today", dayLabel(today, today, Locale.ENGLISH))
        assertEquals("Yesterday", dayLabel(today.minusDays(1), today, Locale.ENGLISH))
        assertEquals("Monday 26 October", dayLabel(today.minusDays(2), today, Locale.ENGLISH))
        assertEquals("26 October 2025", dayLabel(LocalDate.of(2025, 10, 26), today, Locale.ENGLISH))
    }

    // ---- ui state ---------------------------------------------------------------------------

    @Test
    fun `reflection is offered only after a mentor reply and not while streaming`() {
        assertFalse(MentorChatUiState(messages = listOf(msg(1, MessageRole.USER, 28))).canGenerateReflection)
        val answered = listOf(msg(1, MessageRole.USER, 28), msg(2, MessageRole.MENTOR, 28))
        assertTrue(MentorChatUiState(messages = answered).canGenerateReflection)
        assertFalse(MentorChatUiState(messages = answered, isStreaming = true).canGenerateReflection)
        assertTrue(MentorChatUiState().isEmptyConversation)
        assertFalse(MentorChatUiState(isStreaming = true).isEmptyConversation)
    }

    // ---- errors -----------------------------------------------------------------------------

    @Test
    fun `credential errors explain the fix and offer settings`() {
        val missing = AiError.MissingCredentials.toChatError("openai")!!
        assertTrue(missing.detail.contains("No API key is set for OpenAI"))
        assertTrue(missing.opensSettings)
        assertTrue(AiError.InvalidCredentials.toChatError("claude")!!.opensSettings)
        assertFalse(AiError.NetworkUnavailable.toChatError("gemini")!!.opensSettings)
    }

    @Test
    fun `user cancel is not an error and unknown errors keep the code but no raw object`() {
        assertNull(AiError.UserCancelled.toChatError("openai"))
        val unknown = AiError.Unknown("HTTP_500").toChatError("openai")!!
        assertTrue(unknown.detail.contains("HTTP_500"))
        assertFalse(unknown.detail.contains("Unknown("))
    }

    // ---- markdown ---------------------------------------------------------------------------

    @Test
    fun `bold markers are removed and styled`() {
        val out = mentorMarkdown("**Wat maakte deze test het meest stressvol?**")
        assertEquals("Wat maakte deze test het meest stressvol?", out.text)
        assertEquals(FontWeight.Bold, out.spanStyles.single().item.fontWeight)
        assertEquals(0, out.spanStyles.single().start)
        assertEquals(out.text.length, out.spanStyles.single().end)
    }

    @Test
    fun `italic, bullets and headings`() {
        val out = mentorMarkdown("# Focus\n- first *point*\n* second\nplain")
        assertEquals("Focus\n•  first point\n•  second\nplain", out.text)
        assertTrue(out.spanStyles.any { it.item.fontWeight == FontWeight.Bold && out.text.substring(it.start, it.end) == "Focus" })
        assertTrue(out.spanStyles.any { it.item.fontStyle == FontStyle.Italic && out.text.substring(it.start, it.end) == "point" })
    }

    @Test
    fun `literal text without markup is untouched`() {
        val text = "snake_case, 2*3*4 = 24, and a lone ** marker"
        assertEquals(text, mentorMarkdown(text).text)
        assertTrue(mentorMarkdown(text).spanStyles.isEmpty())
    }
}
