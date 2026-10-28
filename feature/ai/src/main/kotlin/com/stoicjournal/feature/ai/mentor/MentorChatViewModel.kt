package com.stoicjournal.feature.ai.mentor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.AiProviderRegistry
import com.stoicjournal.core.domain.model.AiStreamEvent
import com.stoicjournal.core.domain.repository.StoicMentorRepository
import com.stoicjournal.core.domain.usecase.MentorConversationCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MentorChatViewModel @Inject constructor(
    private val coordinator: MentorConversationCoordinator,
    private val mentorRepository: StoicMentorRepository,
    private val providerRegistry: AiProviderRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(MentorChatUiState())
    val uiState: StateFlow<MentorChatUiState> = _uiState.asStateFlow()

    private var activeConversationId: String = UUID.randomUUID().toString()
    private var streamJob: Job? = null

    init {
        loadActiveProvider()
        loadConversation(activeConversationId)
    }

    private fun loadActiveProvider() {
        val provider = providerRegistry.getActiveProvider()
        _uiState.update {
            it.copy(
                activeProviderId = provider?.providerId ?: "openai",
                activeModelId = "default"
            )
        }
    }

    fun loadConversation(conversationId: String) {
        activeConversationId = conversationId
        viewModelScope.launch {
            mentorRepository.getConversation(conversationId).collectLatest { conv ->
                _uiState.update { it.copy(conversation = conv) }
            }
        }
        viewModelScope.launch {
            mentorRepository.getMessagesForConversation(conversationId).collectLatest { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    fun sendMessage(userContent: String) {
        if (userContent.isBlank()) return

        streamJob?.cancel()
        _uiState.update { it.copy(isStreaming = true, streamingText = "", errorMessage = null) }

        streamJob = viewModelScope.launch {
            coordinator.sendMessage(activeConversationId, userContent, _uiState.value.activeMode)
                .collect { event ->
                    when (event) {
                        is AiStreamEvent.Started -> {
                            _uiState.update { it.copy(isStreaming = true, streamingText = "") }
                        }
                        is AiStreamEvent.TextDelta -> {
                            _uiState.update { it.copy(streamingText = it.streamingText + event.text) }
                        }
                        is AiStreamEvent.Completed -> {
                            _uiState.update { it.copy(isStreaming = false, streamingText = "") }
                        }
                        is AiStreamEvent.Failed -> {
                            _uiState.update {
                                it.copy(
                                    isStreaming = false,
                                    errorMessage = "Reflection failed: ${event.error}"
                                )
                            }
                        }
                        else -> {}
                    }
                }
        }
    }

    fun cancelActiveRequest() {
        streamJob?.cancel()
        _uiState.update { it.copy(isStreaming = false, streamingText = "") }
    }

    fun setMode(mode: String) {
        _uiState.update { it.copy(activeMode = mode) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
