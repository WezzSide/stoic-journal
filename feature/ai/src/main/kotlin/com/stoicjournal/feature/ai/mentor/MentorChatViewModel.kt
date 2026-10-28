package com.stoicjournal.feature.ai.mentor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.AiProviderRegistry
import com.stoicjournal.core.domain.model.AiStreamEvent
import com.stoicjournal.core.domain.repository.StoicMentorRepository
import com.stoicjournal.core.domain.usecase.MentorConversationCoordinator
import com.stoicjournal.core.domain.usecase.ReflectionCandidateGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.stoicjournal.core.domain.repository.ReflectionCandidateCache

@HiltViewModel
class MentorChatViewModel @Inject constructor(
    private val coordinator: MentorConversationCoordinator,
    private val mentorRepository: StoicMentorRepository,
    private val providerRegistry: AiProviderRegistry,
    private val candidateGenerator: ReflectionCandidateGenerator,
    private val candidateCache: ReflectionCandidateCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(MentorChatUiState())
    val uiState: StateFlow<MentorChatUiState> = _uiState.asStateFlow()

    private var activeConversationId: String? = null
    private var streamJob: Job? = null
    private var conversationJob: Job? = null
    private var messagesJob: Job? = null

    init {
        loadActiveProvider()
    }

    private fun loadActiveProvider() {
        viewModelScope.launch {
            val provider = providerRegistry.getActiveProvider()
            _uiState.update {
                it.copy(
                    activeProviderId = provider?.providerId ?: "openai",
                    activeModelId = provider?.defaultModelId ?: ""
                )
            }
        }
    }

    /**
     * Binds the screen to one conversation. Previous collectors are cancelled so a stale
     * conversation can never write into the UI state after navigation.
     */
    fun loadConversation(conversationId: String) {
        if (activeConversationId == conversationId && conversationJob?.isActive == true) return
        activeConversationId = conversationId
        conversationJob?.cancel()
        messagesJob?.cancel()
        _uiState.update { it.copy(conversation = null, messages = emptyList(), streamingText = "", errorMessage = null) }
        conversationJob = viewModelScope.launch {
            mentorRepository.getConversation(conversationId).collectLatest { conv ->
                _uiState.update { it.copy(conversation = conv) }
            }
        }
        messagesJob = viewModelScope.launch {
            mentorRepository.getMessagesForConversation(conversationId).collectLatest { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    fun sendMessage(userContent: String) {
        if (userContent.isBlank()) return
        val conversationId = activeConversationId ?: return

        streamJob?.cancel()
        _uiState.update { it.copy(isStreaming = true, streamingText = "", errorMessage = null) }

        streamJob = viewModelScope.launch {
            coordinator.sendMessage(conversationId, userContent, _uiState.value.activeMode)
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
                            loadActiveProvider()
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

    fun generateReflection(onCandidateGenerated: (String) -> Unit) {
        val msgs = _uiState.value.messages
        val conversationId = activeConversationId ?: return
        if (msgs.isEmpty()) return

        _uiState.update { it.copy(isGeneratingCandidate = true, errorMessage = null) }
        viewModelScope.launch {
            val result = candidateGenerator.generateCandidate(
                conversationId = conversationId,
                messages = msgs,
                modelId = _uiState.value.activeModelId
            )
            
            result.onSuccess { candidate ->
                candidateCache.put(candidate)
                _uiState.update { it.copy(isGeneratingCandidate = false, generatedCandidateId = candidate.candidateId) }
                onCandidateGenerated(candidate.candidateId)
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isGeneratingCandidate = false, 
                        errorMessage = "Failed to generate reflection: ${error.message}"
                    ) 
                }
            }
        }
    }
}
