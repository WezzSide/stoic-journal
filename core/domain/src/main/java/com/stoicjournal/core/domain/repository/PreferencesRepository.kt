package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.StoicismLevel
import kotlinx.coroutines.flow.Flow

data class UserPreferences(
    val reminderHour: Int,
    val reminderMinute: Int,
    val reminderEnabled: Boolean,
    val stoicismLevel: StoicismLevel,
    val onboardingComplete: Boolean,
    val hasRequestedNotificationPermission: Boolean = false
)

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setReminderTime(hour: Int, minute: Int)
    suspend fun setReminderEnabled(enabled: Boolean)
    suspend fun setStoicismLevel(level: StoicismLevel)
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setNotificationPermissionRequested(requested: Boolean)
}
