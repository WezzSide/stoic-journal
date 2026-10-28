package com.stoicjournal.feature.ai.mentor.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MentorSuggestedPrompts(
    onSelectMode: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts = listOf(
        "Daily Reflection" to "DAILY_REFLECTION",
        "Evening Review" to "EVENING_REVIEW",
        "Morning Preparation" to "MORNING_PREPARATION",
        "Difficult Decision" to "DIFFICULT_DECISIONS",
        "Gratitude Practice" to "GRATITUDE"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        prompts.forEach { (label, mode) ->
            SuggestionChip(
                onClick = { onSelectMode(label, mode) },
                label = { Text(label) }
            )
        }
    }
}
