package com.stoicjournal.feature.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.Philosopher

@Composable
fun PhilosopherList(
    philosophers: List<Philosopher>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        philosophers.forEach { philosopher ->
            androidx.compose.material3.ListItem(
                headlineContent = { Text(philosopher.name) },
                supportingContent = { Text(philosopher.lived) },
                modifier = Modifier.fillMaxWidth(),
                trailingContent = {
                    if (philosopher.id == selectedId) {
                        Text(
                            text = "Selected",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    }
}
