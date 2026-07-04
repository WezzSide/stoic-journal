package com.stoicjournal.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.domain.repository.UserPreferences
import com.stoicjournal.core.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        preferencesRepository.preferences.onEach { prefs ->
            _uiState.update {
                it.copy(
                    reminderHour = prefs.reminderHour,
                    reminderMinute = prefs.reminderMinute,
                    reminderEnabled = prefs.reminderEnabled,
                    stoicismLevel = prefs.stoicismLevel,
                    onboardingComplete = prefs.onboardingComplete
                )
            }
        }.launchIn(viewModelScope)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setReminderTime(hour, minute)
            if (_uiState.value.reminderEnabled) {
                reminderScheduler.schedule(hour, minute)
            }
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setReminderEnabled(enabled)
            if (enabled) {
                reminderScheduler.schedule(_uiState.value.reminderHour, _uiState.value.reminderMinute)
            } else {
                reminderScheduler.cancel()
            }
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingComplete(false)
        }
    }
}
