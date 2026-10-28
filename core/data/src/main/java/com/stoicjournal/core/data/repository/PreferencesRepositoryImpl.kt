package com.stoicjournal.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.longPreferencesKey
import com.stoicjournal.core.domain.usecase.PinAttemptPolicy
import java.security.MessageDigest
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

        val MORNING_REMINDER_HOUR = intPreferencesKey("morning_reminder_hour")
        val MORNING_REMINDER_MINUTE = intPreferencesKey("morning_reminder_minute")
        val MORNING_REMINDER_ENABLED = booleanPreferencesKey("morning_reminder_enabled")

        val EVENING_REMINDER_HOUR = intPreferencesKey("evening_reminder_hour")
        val EVENING_REMINDER_MINUTE = intPreferencesKey("evening_reminder_minute")
        val EVENING_REMINDER_ENABLED = booleanPreferencesKey("evening_reminder_enabled")

        val STOICISM_LEVEL = intPreferencesKey("stoicism_level")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val HAS_REQUESTED_NOTIFICATION_PERMISSION = booleanPreferencesKey("has_requested_notification_permission")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val PIN_LOCK_ENABLED = booleanPreferencesKey("pin_lock_enabled")
        val PIN_CODE_HASH = stringPreferencesKey("pin_code_hash")
        val LOCK_TIMEOUT_SECONDS = intPreferencesKey("lock_timeout_seconds")
        val PRIVACY_SCREEN_ENABLED = booleanPreferencesKey("privacy_screen_enabled")
        val PIN_FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        val PIN_LOCKOUT_UNTIL = longPreferencesKey("pin_lockout_until_millis")
    }

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            reminderHour = prefs[REMINDER_HOUR] ?: 20,
            reminderMinute = prefs[REMINDER_MINUTE] ?: 0,
            reminderEnabled = prefs[REMINDER_ENABLED] ?: true,
            morningReminderHour = prefs[MORNING_REMINDER_HOUR] ?: 8,
            morningReminderMinute = prefs[MORNING_REMINDER_MINUTE] ?: 0,
            morningReminderEnabled = prefs[MORNING_REMINDER_ENABLED] ?: true,
            eveningReminderHour = prefs[EVENING_REMINDER_HOUR] ?: 21,
            eveningReminderMinute = prefs[EVENING_REMINDER_MINUTE] ?: 0,
            eveningReminderEnabled = prefs[EVENING_REMINDER_ENABLED] ?: true,
            stoicismLevel = StoicismLevel.entries[prefs[STOICISM_LEVEL] ?: 0],
            onboardingComplete = prefs[ONBOARDING_COMPLETE] ?: false,
            hasRequestedNotificationPermission = prefs[HAS_REQUESTED_NOTIFICATION_PERMISSION] ?: false,
            biometricLockEnabled = prefs[BIOMETRIC_LOCK_ENABLED] ?: false,
            pinLockEnabled = prefs[PIN_LOCK_ENABLED] ?: false,
            pinCodeHash = prefs[PIN_CODE_HASH],
            lockTimeoutSeconds = prefs[LOCK_TIMEOUT_SECONDS] ?: 0,
            privacyScreenEnabled = prefs[PRIVACY_SCREEN_ENABLED] ?: false,
            pinFailedAttempts = prefs[PIN_FAILED_ATTEMPTS] ?: 0,
            pinLockoutUntilMillis = prefs[PIN_LOCKOUT_UNTIL] ?: 0L
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

    override suspend fun setMorningReminderTime(hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[MORNING_REMINDER_HOUR] = hour
            prefs[MORNING_REMINDER_MINUTE] = minute
        }
    }

    override suspend fun setMorningReminderEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[MORNING_REMINDER_ENABLED] = enabled
        }
    }

    override suspend fun setEveningReminderTime(hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[EVENING_REMINDER_HOUR] = hour
            prefs[EVENING_REMINDER_MINUTE] = minute
        }
    }

    override suspend fun setEveningReminderEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[EVENING_REMINDER_ENABLED] = enabled
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

    override suspend fun setNotificationPermissionRequested(requested: Boolean) {
        dataStore.edit { prefs ->
            prefs[HAS_REQUESTED_NOTIFICATION_PERMISSION] = requested
        }
    }

    override suspend fun setBiometricLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }

    override suspend fun setPinLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PIN_LOCK_ENABLED] = enabled
        }
    }

    override suspend fun setPinCode(pin: String) {
        val hash = hashPin(pin)
        dataStore.edit { prefs ->
            prefs[PIN_CODE_HASH] = hash
            prefs[PIN_LOCK_ENABLED] = true
        }
    }

    override suspend fun clearPinCode() {
        dataStore.edit { prefs ->
            prefs.remove(PIN_CODE_HASH)
            prefs[PIN_LOCK_ENABLED] = false
        }
    }

    override suspend fun setLockTimeoutSeconds(seconds: Int) {
        dataStore.edit { prefs ->
            prefs[LOCK_TIMEOUT_SECONDS] = seconds
        }
    }

    override suspend fun setPrivacyScreenEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PRIVACY_SCREEN_ENABLED] = enabled
        }
    }

    override suspend fun recordPinFailure(nowMillis: Long) {
        dataStore.edit { prefs ->
            val failures = (prefs[PIN_FAILED_ATTEMPTS] ?: 0) + 1
            prefs[PIN_FAILED_ATTEMPTS] = failures
            val lockout = PinAttemptPolicy.lockoutMillisAfter(failures)
            if (lockout > 0) prefs[PIN_LOCKOUT_UNTIL] = nowMillis + lockout
        }
    }

    override suspend fun resetPinFailures() {
        dataStore.edit { prefs ->
            prefs.remove(PIN_FAILED_ATTEMPTS)
            prefs.remove(PIN_LOCKOUT_UNTIL)
        }
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
