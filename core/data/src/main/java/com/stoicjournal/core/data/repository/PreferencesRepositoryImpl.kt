package com.stoicjournal.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stoic_journal_preferences")

class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private val dataStore = context.dataStore

    companion object {
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val STOICISM_LEVEL = intPreferencesKey("stoicism_level")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            reminderHour = prefs[REMINDER_HOUR] ?: 20,
            reminderMinute = prefs[REMINDER_MINUTE] ?: 0,
            reminderEnabled = prefs[REMINDER_ENABLED] ?: true,
            stoicismLevel = StoicismLevel.entries[prefs[STOICISM_LEVEL] ?: 0],
            onboardingComplete = prefs[ONBOARDING_COMPLETE] ?: false
        )
    }

    override suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[REMINDER_HOUR] = hour
            prefs[REMINDER_MINUTE] = minute
        }
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[REMINDER_ENABLED] = enabled
        }
    }

    override suspend fun setStoicismLevel(level: StoicismLevel) {
        dataStore.edit { prefs ->
            prefs[STOICISM_LEVEL] = level.ordinal
        }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE] = complete
        }
    }
}
