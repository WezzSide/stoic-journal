package com.stoicjournal.feature.today

import androidx.compose.runtime.Composable

@Composable
fun TodayScreen(
    onGuidedJournal: (String) -> Unit,
    onFreeFormJournal: () -> Unit,
    onExercise: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
}
