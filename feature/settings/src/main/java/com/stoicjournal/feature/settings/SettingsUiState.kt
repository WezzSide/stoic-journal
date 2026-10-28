package com.stoicjournal.feature.settings

import com.stoicjournal.core.domain.model.StoicismLevel

data class SettingsUiState(
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val reminderEnabled: Boolean = true,
    val morningReminderHour: Int = 8,
    val morningReminderMinute: Int = 0,
    val morningReminderEnabled: Boolean = true,
    val eveningReminderHour: Int = 21,
    val eveningReminderMinute: Int = 0,
    val eveningReminderEnabled: Boolean = true,
    val stoicismLevel: StoicismLevel = StoicismLevel.BEGINNER,
    val onboardingComplete: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val pinLockEnabled: Boolean = false,
    val hasPinSet: Boolean = false,
    val lockTimeoutSeconds: Int = 0,
    val privacyScreenEnabled: Boolean = false,
    // AI providers
    val activeProviderId: String = "openai",
    val activeProviderExplicitlyChosen: Boolean = false,
    val configuredProviderIds: Set<String> = emptySet(),
    // Proactive Stoic Mentor
    val proactiveMentorEnabled: Boolean = false,
    val proactiveReminderHour: Int = 20,
    val proactiveReminderMinute: Int = 30,
    val proactiveQuietStartHour: Int = 22,
    val proactiveQuietEndHour: Int = 8,
    val error: String? = null
)
