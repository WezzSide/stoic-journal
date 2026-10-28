package com.stoicjournal.core.domain.repository

import kotlinx.coroutines.flow.Flow

data class ProactiveMentorPreferences(
    val proactiveMentorEnabled: Boolean = false,
    val preferredReminderHour: Int = 20,
    val preferredReminderMinute: Int = 30,
    val quietHoursStartHour: Int = 22,
    val quietHoursEndHour: Int = 8,
    val maxDailyInvitations: Int = 1,
    val snoozedUntilTimestamp: Long? = null,
    val skippedTodayDate: String? = null
)

interface ProactiveMentorPreferencesRepository {
    val preferences: Flow<ProactiveMentorPreferences>
    suspend fun setProactiveMentorEnabled(enabled: Boolean)
    suspend fun setPreferredReminderTime(hour: Int, minute: Int)
    suspend fun setQuietHours(startHour: Int, endHour: Int)
    suspend fun setSnoozedUntil(timestamp: Long?)
    suspend fun setSkippedTodayDate(dateString: String?)
}
