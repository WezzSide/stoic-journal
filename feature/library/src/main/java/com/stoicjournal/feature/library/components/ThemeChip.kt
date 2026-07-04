package com.stoicjournal.feature.library.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.Theme
import java.util.Locale

@Composable
fun ThemeChip(
    theme: Theme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(theme.name.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) }) },
        modifier = modifier.heightIn(min = 48.dp)
    )
}
