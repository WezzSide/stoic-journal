package com.stoicjournal.feature.ai.mentor

import com.stoicjournal.core.domain.model.MentorMessage
import com.stoicjournal.core.domain.model.MessageRole
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One row of the chat list. Built by [buildChatItems]; rendered by MentorChatScreen. */
sealed interface ChatItem {
    val key: String

    data class DayHeader(val date: LocalDate) : ChatItem {
        override val key = "day-$date"
    }

    /**
     * @param isFirstInGroup first of consecutive messages from the same sender on the same day (gets more top spacing)
     * @param isLastInGroup last of such a run (gets the bubble "tail" corner)
     */
    data class Message(
        val message: MentorMessage,
        val isFirstInGroup: Boolean,
        val isLastInGroup: Boolean
    ) : ChatItem {
        override val key = message.id
    }

    /** The mentor has been asked but no text has arrived yet ("typing…" dots). */
    data object Typing : ChatItem {
        override val key = "typing"
    }

    /** Partial mentor reply while it is streaming in. */
    data class Streaming(val text: String) : ChatItem {
        override val key = "streaming"
    }
}

/**
 * Builds the chat rows in chronological order (oldest first): a day header before the first message of
 * each calendar day, sender grouping, and a trailing typing/streaming row while a reply is in progress.
 * SYSTEM messages are never shown. The screen renders the reversed list in a reverse-layout LazyColumn so
 * the conversation stays anchored to the bottom, like other chat apps.
 */
fun buildChatItems(
    messages: List<MentorMessage>,
    isStreaming: Boolean,
    streamingText: String,
    zone: ZoneId = ZoneId.systemDefault()
): List<ChatItem> {
    val visible = messages.filter { it.role != MessageRole.SYSTEM }.sortedBy { it.sequenceNumber }
    val items = mutableListOf<ChatItem>()
    var previousDay: LocalDate? = null
    visible.forEachIndexed { index, message ->
        val day = message.timestamp.atZone(zone).toLocalDate()
        if (day != previousDay) {
            items += ChatItem.DayHeader(day)
            previousDay = day
        }
        val prev = visible.getOrNull(index - 1)
        val next = visible.getOrNull(index + 1)
        val sameAsPrev = prev != null && prev.role == message.role && prev.timestamp.atZone(zone).toLocalDate() == day
        val sameAsNext = next != null && next.role == message.role && next.timestamp.atZone(zone).toLocalDate() == day
        items += ChatItem.Message(message, isFirstInGroup = !sameAsPrev, isLastInGroup = !sameAsNext)
    }
    if (isStreaming) {
        items += if (streamingText.isEmpty()) ChatItem.Typing else ChatItem.Streaming(streamingText)
    }
    return items
}

/** "Today", "Yesterday", or e.g. "Monday 26 October" (same year) / "26 October 2025". */
fun dayLabel(date: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> {
        val pattern = if (date.year == today.year) "EEEE d MMMM" else "d MMMM yyyy"
        DateTimeFormatter.ofPattern(pattern, locale).format(date)
    }
}
