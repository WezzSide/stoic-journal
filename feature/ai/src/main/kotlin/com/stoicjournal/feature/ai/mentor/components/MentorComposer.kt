package com.stoicjournal.feature.ai.mentor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/**
 * Messenger-style input bar: a rounded, growing text field (up to 6 lines) and one round action button
 * that changes with context — microphone when empty, send when there is text, stop while the mentor is
 * answering. The text is hoisted so the screen can keep the draft across configuration changes and
 * insert dictated text for review before sending.
 */
@Composable
fun MentorComposer(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    onCancelClick: () -> Unit,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f)
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Message Stoic Mentor") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                maxLines = 6,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }

        val buttonModifier = Modifier.size(52.dp)
        when {
            isStreaming -> FilledIconButton(
                onClick = onCancelClick,
                modifier = buttonModifier,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop Generation")
            }
            text.isNotBlank() -> FilledIconButton(onClick = onSend, modifier = buttonModifier) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
            }
            else -> FilledIconButton(onClick = onVoiceClick, modifier = buttonModifier) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Input")
            }
        }
    }
}
