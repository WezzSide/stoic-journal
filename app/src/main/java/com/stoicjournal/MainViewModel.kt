package com.stoicjournal

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.data.seed.AssetSeedImporter
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.notifications.ReminderScheduler
import com.stoicjournal.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val assetSeedImporter: AssetSeedImporter,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _startDestination = MutableStateFlow<Screen?>(null)
    val startDestination: StateFlow<Screen?> = _startDestination

    init {
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
                    reminderScheduler.schedule(prefs.reminderHour, prefs.reminderMinute)
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

    private companion object {
        const val TAG = "MainViewModel"
    }
}
