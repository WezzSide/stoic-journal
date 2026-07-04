package com.stoicjournal.feature.today

import com.stoicjournal.core.domain.model.Exercise
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Streak

data class TodayUiState(
    val prompt: Prompt? = null,
    val quote: Quote? = null,
    val streak: Streak = Streak(0, null),
    val exercises: List<Exercise> = emptyList(),
    val hasRequestedNotificationPermission: Boolean = false,
    val isLoading: Boolean = true
)
