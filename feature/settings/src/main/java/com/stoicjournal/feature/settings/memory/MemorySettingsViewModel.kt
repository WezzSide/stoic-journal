package com.stoicjournal.feature.settings.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.MentorMemory
import com.stoicjournal.core.domain.model.MentorMemoryCategory
import com.stoicjournal.core.domain.model.MentorMemoryStatus
import com.stoicjournal.core.domain.repository.MentorMemoryPreferences
import com.stoicjournal.core.domain.repository.MentorMemoryPreferencesRepository
import com.stoicjournal.core.domain.repository.MentorMemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class MemorySettingsUiState(
    val preferences: MentorMemoryPreferences = MentorMemoryPreferences(),
    val memories: List<MentorMemory> = emptyList(),
    val error: String? = null
)

/**
 * Review surface for the contextual-memory feature (AI-7). Every flag defaults to off and the
 * provider only ever sees approved memories when the user has explicitly turned on
 * "send historical context to provider" (enforced in HistoricalReflectionRetriever).
 */
@HiltViewModel
class MemorySettingsViewModel @Inject constructor(
    private val memoryRepository: MentorMemoryRepository,
    private val preferencesRepository: MentorMemoryPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemorySettingsUiState())
    val uiState: StateFlow<MemorySettingsUiState> = _uiState.asStateFlow()

    init {
        preferencesRepository.preferences
            .onEach { prefs -> _uiState.update { it.copy(preferences = prefs) } }
            .launchIn(viewModelScope)
        memoryRepository.getAllMemories()
            .onEach { list -> _uiState.update { it.copy(memories = list) } }
            .launchIn(viewModelScope)
    }

    fun setMemoryEnabled(enabled: Boolean) = edit { preferencesRepository.setMemoryEnabled(enabled) }
    fun setHistoricalJournalAccessEnabled(enabled: Boolean) = edit { preferencesRepository.setHistoricalJournalAccessEnabled(enabled) }
    fun setUseApprovedMemories(enabled: Boolean) = edit { preferencesRepository.setUseApprovedMemories(enabled) }
    fun setSendHistoricalContextToProvider(enabled: Boolean) = edit { preferencesRepository.setSendHistoricalContextToProvider(enabled) }

    /** Creates a user-authored memory proposal; it is not used until the user approves it. */
    fun proposeMemory(content: String, category: MentorMemoryCategory = MentorMemoryCategory.USER_AUTHORED_INSIGHT) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        edit {
            memoryRepository.saveMemory(
                MentorMemory(
                    id = UUID.randomUUID().toString(),
                    category = category,
                    content = trimmed,
                    status = MentorMemoryStatus.PROPOSED,
                    userApproved = false
                )
            )
        }
    }

    fun approveMemory(id: String) = edit { memoryRepository.approveMemory(id) }

    fun deleteMemory(id: String) = edit { memoryRepository.deleteMemory(id) }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun edit(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Memory update failed") }
            }
        }
    }
}
