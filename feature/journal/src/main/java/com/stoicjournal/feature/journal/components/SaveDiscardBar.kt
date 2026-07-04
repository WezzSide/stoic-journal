package com.stoicjournal.feature.journal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SaveDiscardBar(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    saveEnabled: Boolean,
    isSaving: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDiscard,
            enabled = !isSaving,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
        ) {
            Text("Discard")
        }
        Button(
            onClick = onSave,
            enabled = saveEnabled && !isSaving,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
        ) {
            Text(if (isSaving) "Saving…" else "Save")
        }
    }
}
