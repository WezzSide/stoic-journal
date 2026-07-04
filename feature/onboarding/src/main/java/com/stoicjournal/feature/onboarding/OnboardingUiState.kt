package com.stoicjournal.feature.onboarding

import com.stoicjournal.core.domain.model.StoicismLevel

data class OnboardingUiState(
    val step: Int = 0,
    val selectedLevel: StoicismLevel = StoicismLevel.BEGINNER,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val isLoading: Boolean = false
)
