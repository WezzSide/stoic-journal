package com.stoicjournal

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.data.seed.AssetSeedImporter
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.UserPreferences
import com.stoicjournal.core.notifications.ReminderScheduler
import com.stoicjournal.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val assetSeedImporter: AssetSeedImporter,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _startDestination = MutableStateFlow<Screen?>(null)
    val startDestination: StateFlow<Screen?> = _startDestination

    private val _userPreferences = MutableStateFlow<UserPreferences?>(null)
    val userPreferences: StateFlow<UserPreferences?> = _userPreferences.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var backgroundTimestampMillis: Long? = null

    init {
        preferencesRepository.preferences.onEach { prefs ->
            val isFirstTime = _userPreferences.value == null
            _userPreferences.value = prefs

            if (isFirstTime && (prefs.biometricLockEnabled || prefs.pinLockEnabled)) {
                _isLocked.value = true
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            try {
                try {
                    assetSeedImporter.seedIfEmpty()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to seed bundled content", e)
                }
                val prefs = preferencesRepository.preferences.first()
                if (prefs.reminderEnabled) {
                    try {
                        reminderScheduler.schedule(prefs.reminderHour, prefs.reminderMinute)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to schedule reminder", e)
                    }
                }
                _startDestination.value = if (prefs.onboardingComplete) {
                    Screen.Today
                } else {
                    Screen.Onboarding
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize start destination", e)
                _startDestination.value = Screen.Onboarding
            }
        }
    }

    fun onAppBackgrounded() {
        backgroundTimestampMillis = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        val lastBg = backgroundTimestampMillis
        val prefs = _userPreferences.value ?: return
        if (lastBg != null && (prefs.biometricLockEnabled || prefs.pinLockEnabled)) {
            val elapsedSeconds = (System.currentTimeMillis() - lastBg) / 1000
            if (elapsedSeconds >= prefs.lockTimeoutSeconds) {
                _isLocked.value = true
            }
        }
        backgroundTimestampMillis = null
    }

    fun unlock() {
        _isLocked.value = false
    }

    fun unlockWithPin(pin: String): Boolean {
        val storedHash = _userPreferences.value?.pinCodeHash ?: return false
        val enteredHash = MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }

        return if (storedHash == enteredHash) {
            _isLocked.value = false
            true
        } else {
            false
        }
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
