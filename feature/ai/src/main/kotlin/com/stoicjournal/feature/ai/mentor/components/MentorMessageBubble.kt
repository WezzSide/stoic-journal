package com.stoicjournal.feature.ai.mentor.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.MentorMessage
import com.stoicjournal.core.domain.model.MessageRole
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Chat bubble in the style of common messengers: the user's messages on the right in the brand colour,
 * the mentor's on the left on a neutral surface. Bubbles are at most ~80 % of the width, show the time,
 * and only the last bubble of a run from the same sender gets the pointed "tail" corner.
 * Text is selectable so it can be copied.
 */
@Composable
fun MentorMessageBubble(
    message: MentorMessage,
    modifier: Modifier = Modifier,
    isLastInGroup: Boolean = true,
    isStreaming: Boolean = false
) {
    ChatBubble(
        isUser = message.role == MessageRole.USER,
        isLastInGroup = isLastInGroup,
        modifier = modifier
    ) { contentColor ->
        val isUser = message.role == MessageRole.USER
        val body = if (isUser) {
            androidx.compose.ui.text.AnnotatedString(message.content)
        } else {
            com.stoicjournal.feature.ai.mentor.mentorMarkdown(message.content)
        }
        SelectionContainer {
            Text(
                text = if (isStreaming) body + androidx.compose.ui.text.AnnotatedString(" ▍") else body,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
        }
        if (!isStreaming) {
            Text(
                text = timeFormatter.format(message.timestamp.atZone(ZoneId.systemDefault())),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 2.dp)
            )
        }
    }
}

/** Mentor bubble with three pulsing dots, shown while waiting for the first streamed text. */
@Composable
fun MentorTypingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    ChatBubble(
        isUser = false,
        isLastInGroup = true,
        modifier = modifier.semantics { contentDescription = "Stoic Mentor is typing" }
    ) { contentColor ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
        ) {
            repeat(3) { index ->
                val alpha by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 500, delayMillis = index * 160),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot$index"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(alpha)
                        .background(contentColor, CircleShape)
                )
            }
        }
    }
}

/** Centered date chip between days ("Today", "Yesterday", …). */
@Composable
fun ChatDayHeader(label: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ChatBubble(
    isUser: Boolean,
    isLastInGroup: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.(contentColor: Color) -> Unit
) {
    val container = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val tail: Dp = if (isLastInGroup) 4.dp else 18.dp
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isUser) 18.dp else tail,
        bottomEnd = if (isUser) tail else 18.dp
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) Spacer(Modifier.width(48.dp))
        Surface(
            shape = shape,
            color = container,
            modifier = Modifier.widthIn(min = 64.dp).weight(1f, fill = false)
        ) {
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)) {
                content(contentColor)
            }
        }
        if (!isUser) Spacer(Modifier.width(48.dp))
    }
}

/** Convenience for previews/tests. */
internal fun previewMessage(role: MessageRole, text: String, seq: Int = 1) = MentorMessage(
    id = "preview-$seq",
    conversationId = "preview",
    role = role,
    content = text,
    timestamp = Instant.now(),
    sequenceNumber = seq
)
