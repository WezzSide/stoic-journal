package com.stoicjournal.feature.today.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QuickActionsRow(
    onFreeForm: () -> Unit,
    onExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onFreeForm, modifier = Modifier.weight(1f)) {
            Text("Free-form")
        }
        Button(onClick = onExercise, modifier = Modifier.weight(1f)) {
            Text("Exercise")
        }
    }
}
