package com.stoicjournal.feature.ai.mentor

import com.stoicjournal.core.domain.model.MentorConversation
import com.stoicjournal.core.domain.model.MentorMessage
import com.stoicjournal.core.domain.model.MessageRole

data class MentorChatUiState(
    val conversation: MentorConversation? = null,
    val messages: List<MentorMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val streamingText: String = "",
    val activeProviderId: String = "openai",
    val activeModelId: String = "",
    val activeMode: String = "DAILY_REFLECTION",
    val error: MentorChatError? = null,
    val isGeneratingCandidate: Boolean = false,
    val generatedCandidateId: String? = null
) {
    /** A reflection can be generated once the mentor has answered at least once and nothing is in flight. */
    val canGenerateReflection: Boolean
        get() = messages.any { it.role == MessageRole.MENTOR } && !isStreaming

    val isEmptyConversation: Boolean
        get() = messages.none { it.role != MessageRole.SYSTEM } && !isStreaming
}
