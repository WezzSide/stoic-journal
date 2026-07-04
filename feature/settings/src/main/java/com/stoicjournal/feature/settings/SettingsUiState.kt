package com.stoicjournal.feature.settings

import com.stoicjournal.core.domain.model.StoicismLevel

data class SettingsUiState(
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val reminderEnabled: Boolean = true,
    val stoicismLevel: StoicismLevel = StoicismLevel.BEGINNER,
    val onboardingComplete: Boolean = false
)
