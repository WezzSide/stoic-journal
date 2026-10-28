package com.stoicjournal.feature.journal.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.Prompt

@Composable
fun PromptSelectionBar(
    prompts: List<Prompt>,
    onRandomPromptClick: () -> Unit,
    onPromptSelect: (Prompt) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onRandomPromptClick,
            label = { Text("Geef mij een vraag 💡") },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        )

        for (prompt in prompts) {
            val shortLabel = when {
                prompt.text.contains("Ochtend") -> "🌅 Ochtend"
                prompt.text.contains("Seneca") -> "🌙 Avondreview"
                prompt.text.contains("Dichotomie") -> "⚖️ Controle"
                prompt.text.contains("Amor Fati") -> "🍇 Amor Fati"
                prompt.text.contains("Memento Mori") -> "⏳ Memento Mori"
                else -> prompt.theme.name.lowercase().replaceFirstChar { it.uppercase() }
            }
            AssistChip(
                onClick = { onPromptSelect(prompt) },
                label = { Text(shortLabel) }
            )
        }
    }
}
