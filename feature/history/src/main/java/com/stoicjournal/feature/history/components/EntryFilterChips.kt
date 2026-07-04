package com.stoicjournal.feature.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.EntryType

@Composable
fun EntryFilterChips(
    selectedFilter: EntryType?,
    onSelect: (EntryType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == null,
            onClick = { onSelect(null) },
            label = { Text("All") }
        )
        EntryType.entries.forEach { type ->
            FilterChip(
                selected = selectedFilter == type,
                onClick = { onSelect(type) },
                label = { Text(type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}
