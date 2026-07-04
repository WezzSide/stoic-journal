package com.stoicjournal.feature.journal.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.Quote

@Composable
fun QuoteHeader(quote: Quote?, modifier: Modifier = Modifier) {
    if (quote == null) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "\"${quote.text}\"",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "— ${quote.attribution}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
