package com.stoicjournal.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.stoicjournal.core.domain.repository.ProactiveMentorPreferences
import com.stoicjournal.core.domain.repository.ProactiveMentorPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.proactiveDataStore: DataStore<Preferences> by preferencesDataStore(name = "proactive_mentor_preferences")

@Singleton
class ProactiveMentorPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ProactiveMentorPreferencesRepository {

    private object Keys {
        val ENABLED = booleanPreferencesKey("proactive_mentor_enabled")
        val REMINDER_HOUR = intPreferencesKey("proactive_reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("proactive_reminder_minute")
        val QUIET_START = intPreferencesKey("proactive_quiet_start")
        val QUIET_END = intPreferencesKey("proactive_quiet_end")
        val SNOOZED_UNTIL = longPreferencesKey("proactive_snoozed_until")
        val SKIPPED_DATE = stringPreferencesKey("proactive_skipped_date")
    }

    override val preferences: Flow<ProactiveMentorPreferences> = context.proactiveDataStore.data.map { prefs ->
        ProactiveMentorPreferences(
            proactiveMentorEnabled = prefs[Keys.ENABLED] ?: false,
            preferredReminderHour = prefs[Keys.REMINDER_HOUR] ?: 20,
            preferredReminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 30,
            quietHoursStartHour = prefs[Keys.QUIET_START] ?: 22,
            quietHoursEndHour = prefs[Keys.QUIET_END] ?: 8,
            snoozedUntilTimestamp = prefs[Keys.SNOOZED_UNTIL],
            skippedTodayDate = prefs[Keys.SKIPPED_DATE]
        )
    }

    override suspend fun setProactiveMentorEnabled(enabled: Boolean) {
        context.proactiveDataStore.edit { it[Keys.ENABLED] = enabled }
    }

    override suspend fun setPreferredReminderTime(hour: Int, minute: Int) {
        context.proactiveDataStore.edit {
            it[Keys.REMINDER_HOUR] = hour
            it[Keys.REMINDER_MINUTE] = minute
        }
    }

    override suspend fun setQuietHours(startHour: Int, endHour: Int) {
        context.proactiveDataStore.edit {
            it[Keys.QUIET_START] = startHour
            it[Keys.QUIET_END] = endHour
        }
    }

    override suspend fun setSnoozedUntil(timestamp: Long?) {
        context.proactiveDataStore.edit {
            if (timestamp == null) it.remove(Keys.SNOOZED_UNTIL)
            else it[Keys.SNOOZED_UNTIL] = timestamp
        }
    }

    override suspend fun setSkippedTodayDate(dateString: String?) {
        context.proactiveDataStore.edit {
            if (dateString == null) it.remove(Keys.SKIPPED_DATE)
            else it[Keys.SKIPPED_DATE] = dateString
        }
    }
}
