package com.stoicjournal.feature.journal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.Virtue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VirtueTagPicker(
    selectedVirtues: List<Virtue>,
    onVirtueToggle: (Virtue) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = "Beoefende Stoïcijnse Deugden:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            for (virtue in Virtue.entries) {
                val isSelected = selectedVirtues.contains(virtue)
                FilterChip(
                    selected = isSelected,
                    onClick = { onVirtueToggle(virtue) },
                    label = { Text("${virtue.symbol} ${virtue.dutchName}") }
                )
            }
        }
    }
}
