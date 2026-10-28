package com.stoicjournal.feature.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.model.Virtue
import com.stoicjournal.core.domain.model.AiProviderRegistry
import com.stoicjournal.core.domain.model.ProviderCredential
import com.stoicjournal.core.domain.repository.SecureCredentialRepository
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val journalRepository: JournalRepository,
    private val reminderScheduler: ReminderScheduler,
    private val secureCredentialRepository: SecureCredentialRepository,
    private val aiProviderRegistry: AiProviderRegistry
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
                    morningReminderHour = prefs.morningReminderHour,
                    morningReminderMinute = prefs.morningReminderMinute,
                    morningReminderEnabled = prefs.morningReminderEnabled,
                    eveningReminderHour = prefs.eveningReminderHour,
                    eveningReminderMinute = prefs.eveningReminderMinute,
                    eveningReminderEnabled = prefs.eveningReminderEnabled,
                    stoicismLevel = prefs.stoicismLevel,
                    onboardingComplete = prefs.onboardingComplete,
                    biometricLockEnabled = prefs.biometricLockEnabled,
                    pinLockEnabled = prefs.pinLockEnabled,
                    hasPinSet = !prefs.pinCodeHash.isNull_or_empty(),
                    lockTimeoutSeconds = prefs.lockTimeoutSeconds,
                    privacyScreenEnabled = prefs.privacyScreenEnabled
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                preferencesRepository.setReminderTime(hour, minute)
                if (_uiState.value.reminderEnabled) {
                    reminderScheduler.schedule(hour, minute)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set reminder time", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to set reminder time") }
            }
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setReminderEnabled(enabled)
                if (enabled) {
                    reminderScheduler.schedule(_uiState.value.reminderHour, _uiState.value.reminderMinute)
                } else {
                    reminderScheduler.cancel()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set reminder enabled state", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to update reminder") }
            }
        }
    }

    fun setMorningReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                preferencesRepository.setMorningReminderTime(hour, minute)
                if (_uiState.value.morningReminderEnabled) {
                    reminderScheduler.scheduleMorning(hour, minute)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set morning reminder time", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to set morning reminder time") }
            }
        }
    }

    fun setMorningReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setMorningReminderEnabled(enabled)
                if (enabled) {
                    reminderScheduler.scheduleMorning(_uiState.value.morningReminderHour, _uiState.value.morningReminderMinute)
                } else {
                    reminderScheduler.cancelMorning()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set morning reminder enabled state", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to update morning reminder") }
            }
        }
    }

    fun setEveningReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                preferencesRepository.setEveningReminderTime(hour, minute)
                if (_uiState.value.eveningReminderEnabled) {
                    reminderScheduler.scheduleEvening(hour, minute)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set evening reminder time", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to set evening reminder time") }
            }
        }
    }

    fun setEveningReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setEveningReminderEnabled(enabled)
                if (enabled) {
                    reminderScheduler.scheduleEvening(_uiState.value.eveningReminderHour, _uiState.value.eveningReminderMinute)
                } else {
                    reminderScheduler.cancelEvening()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set evening reminder enabled state", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to update evening reminder") }
            }
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setBiometricLockEnabled(enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set biometric lock enabled", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to update biometric lock") }
            }
        }
    }

    fun setPinLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setPinLockEnabled(enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set PIN lock enabled", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to update PIN lock") }
            }
        }
    }

    fun setPinCode(pin: String) {
        viewModelScope.launch {
            try {
                preferencesRepository.setPinCode(pin)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set PIN code", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to set PIN code") }
            }
        }
    }

    fun clearPinCode() {
        viewModelScope.launch {
            try {
                preferencesRepository.clearPinCode()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear PIN code", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to clear PIN code") }
            }
        }
    }

    fun setLockTimeoutSeconds(seconds: Int) {
        viewModelScope.launch {
            try {
                preferencesRepository.setLockTimeoutSeconds(seconds)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set lock timeout", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to set lock timeout") }
            }
        }
    }

    fun setPrivacyScreenEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.setPrivacyScreenEnabled(enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set privacy screen enabled", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to update privacy screen") }
            }
        }
    }

    fun exportBackup(uri: Uri, context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entries = journalRepository.getAllEntries().first()
                val jsonArray = JSONArray()
                for (entry in entries) {
                    val obj = JSONObject().apply {
                        put("id", entry.id)
                        put("type", entry.type.name)
                        put("promptId", entry.promptId ?: "")
                        put("title", entry.title ?: "")
                        put("body", entry.body)
                        put("quoteId", entry.quoteId ?: "")
                        put("exerciseId", entry.exerciseId ?: "")
                        put("virtues", JSONArray(entry.virtues.map { it.name }))
                        put("createdAt", entry.createdAt.toEpochMilli())
                        put("updatedAt", entry.updatedAt.toEpochMilli())
                    }
                    jsonArray.put(obj)
                }

                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(jsonArray.toString(2).toByteArray())
                }
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export backup", e)
                _uiState.update { it.copy(error = "Exporteren mislukt: ${e.message}") }
            }
        }
    }

    fun importBackup(uri: Uri, context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: return@launch

                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val virtuesArray = obj.optJSONArray("virtues")
                    val virtueList = mutableListOf<Virtue>()
                    if (virtuesArray != null) {
                        for (j in 0 until virtuesArray.length()) {
                            try { virtueList.add(Virtue.valueOf(virtuesArray.getString(j))) } catch (_: Exception) {}
                        }
                    }

                    val entry = JournalEntry(
                        id = obj.getString("id"),
                        type = EntryType.valueOf(obj.getString("type")),
                        promptId = obj.optString("promptId").takeIf { it.isNotBlank() },
                        title = obj.optString("title").takeIf { it.isNotBlank() },
                        body = obj.getString("body"),
                        quoteId = obj.optString("quoteId").takeIf { it.isNotBlank() },
                        exerciseId = obj.optString("exerciseId").takeIf { it.isNotBlank() },
                        virtues = virtueList,
                        createdAt = Instant.ofEpochMilli(obj.getLong("createdAt")),
                        updatedAt = Instant.ofEpochMilli(obj.getLong("updatedAt"))
                    )
                    journalRepository.saveEntry(entry)
                }
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import backup", e)
                _uiState.update { it.copy(error = "Importeren mislukt: ${e.message}") }
            }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingComplete(false)
        }
    }

    fun saveAiApiKey(providerId: String, apiKey: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                secureCredentialRepository.saveCredential(providerId, ProviderCredential.ApiKey(apiKey))
                val provider = aiProviderRegistry.getProvider(providerId)
                if (provider != null) {
                    val result = provider.validateConnection()
                    if (result.isSuccess) {
                        onResult(true, "Verbinding succesvol! 🤖")
                    } else {
                        onResult(false, "Verbinding mislukt: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    onResult(true, "API key veilig opgeslagen.")
                }
            } catch (e: Exception) {
                onResult(false, "Opslaan mislukt: ${e.message}")
            }
        }
    }

    fun setNotificationPermissionRequested(requested: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationPermissionRequested(requested)
        }
    }

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
