package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.StoicismLevel
import kotlinx.coroutines.flow.Flow

data class UserPreferences(
    val reminderHour: Int,
    val reminderMinute: Int,
    val reminderEnabled: Boolean,
    val morningReminderHour: Int = 8,
    val morningReminderMinute: Int = 0,
    val morningReminderEnabled: Boolean = true,
    val eveningReminderHour: Int = 21,
    val eveningReminderMinute: Int = 0,
    val eveningReminderEnabled: Boolean = true,
    val stoicismLevel: StoicismLevel,
    val onboardingComplete: Boolean,
    val hasRequestedNotificationPermission: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val pinLockEnabled: Boolean = false,
    val pinCodeHash: String? = null,
    val lockTimeoutSeconds: Int = 0,
    val privacyScreenEnabled: Boolean = false
)

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setReminderTime(hour: Int, minute: Int)
    suspend fun setReminderEnabled(enabled: Boolean)
    suspend fun setMorningReminderTime(hour: Int, minute: Int)
    suspend fun setMorningReminderEnabled(enabled: Boolean)
    suspend fun setEveningReminderTime(hour: Int, minute: Int)
    suspend fun setEveningReminderEnabled(enabled: Boolean)
    suspend fun setStoicismLevel(level: StoicismLevel)
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setNotificationPermissionRequested(requested: Boolean)
    suspend fun setBiometricLockEnabled(enabled: Boolean)
    suspend fun setPinLockEnabled(enabled: Boolean)
    suspend fun setPinCode(pin: String)
    suspend fun clearPinCode()
    suspend fun setLockTimeoutSeconds(seconds: Int)
    suspend fun setPrivacyScreenEnabled(enabled: Boolean)
}
