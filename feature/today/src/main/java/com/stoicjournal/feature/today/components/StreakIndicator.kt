package com.stoicjournal.feature.today.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stoicjournal.core.domain.model.Streak

@Composable
fun StreakIndicator(streak: Streak) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag("StreakIndicator")
    ) {
        Text(
            text = "${streak.currentCount}",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 32.sp
        )
        Text(
            text = " day streak",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
