package com.stoicjournal.feature.ai.mentor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** A guided starting point; [mode] is sent to the coordinator as the conversation mode. */
data class MentorStarter(val label: String, val description: String, val mode: String, val icon: ImageVector)

val mentorStarters = listOf(
    MentorStarter("Daily Reflection", "Look back on what happened today", "DAILY_REFLECTION", Icons.Outlined.EditNote),
    MentorStarter("Morning Preparation", "Set your intention for the day ahead", "MORNING_PREPARATION", Icons.Filled.WbSunny),
    MentorStarter("Evening Review", "Review your actions and judgements", "EVENING_REVIEW", Icons.Filled.NightsStay),
    MentorStarter("Difficult Decision", "Think through a choice with a clear head", "DIFFICULT_DECISIONS", Icons.Filled.Balance),
    MentorStarter("Gratitude Practice", "Notice what you already have", "GRATITUDE", Icons.Filled.Favorite)
)

/** Round avatar used in the top bar and the empty state. */
@Composable
fun MentorAvatar(size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

/**
 * Shown before the first message: explains in plain words what the mentor does and what is expected of
 * the user, and offers guided starting points. Disappears once the conversation starts, so the chat
 * itself stays uncluttered.
 */
@Composable
fun MentorEmptyState(
    onStarterSelected: (MentorStarter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MentorAvatar(size = 72.dp)
        Spacer(Modifier.height(12.dp))
        Text("Your Stoic Mentor", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tell the mentor what is on your mind: something that happened, a worry, or a decision. " +
                "You will get questions and Stoic perspectives, not judgement.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "When you are done, save the conversation to your journal as a reflection.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Choose a starting point, or just type below",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            mentorStarters.forEach { starter ->
                OutlinedCard(
                    onClick = { onStarterSelected(starter) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(starter.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(starter.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                starter.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
