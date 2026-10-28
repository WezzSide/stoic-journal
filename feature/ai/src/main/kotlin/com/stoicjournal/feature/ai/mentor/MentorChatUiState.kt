package com.stoicjournal.feature.ai.mentor

import com.stoicjournal.core.domain.model.MentorConversation
import com.stoicjournal.core.domain.model.MentorMessage

data class MentorChatUiState(
    val conversation: MentorConversation? = null,
    val messages: List<MentorMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val streamingText: String = "",
    val activeProviderId: String = "openai",
    val activeModelId: String = "gpt-4o",
    val activeMode: String = "DAILY_REFLECTION",
    val errorMessage: String? = null
)
