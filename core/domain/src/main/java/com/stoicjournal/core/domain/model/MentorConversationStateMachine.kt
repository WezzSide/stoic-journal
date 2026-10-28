package com.stoicjournal.core.domain.model

sealed interface MentorEvent {
    data object UserSentMessage : MentorEvent
    data object ProviderConnected : MentorEvent
    data object StartStreaming : MentorEvent
    data object StreamCompleted : MentorEvent
    data object UserCancelled : MentorEvent
    data class ErrorOccurred(val error: AiError) : MentorEvent
    data object DraftReflection : MentorEvent
    data object ApproveReflection : MentorEvent
}

class MentorConversationStateMachine(initialState: ConversationState = ConversationState.CREATED) {
    var currentState: ConversationState = initialState
        private set

    fun transition(event: MentorEvent): ConversationState {
        currentState = when (currentState) {
            ConversationState.CREATED -> when (event) {
                MentorEvent.UserSentMessage -> ConversationState.WAITING_FOR_PROVIDER
                else -> currentState
            }
            ConversationState.ACTIVE -> when (event) {
                MentorEvent.UserSentMessage -> ConversationState.WAITING_FOR_PROVIDER
                MentorEvent.DraftReflection -> ConversationState.REFLECTION_DRAFTED
                else -> currentState
            }
            ConversationState.WAITING_FOR_PROVIDER -> when (event) {
                MentorEvent.StartStreaming -> ConversationState.STREAMING_RESPONSE
                MentorEvent.UserCancelled -> ConversationState.ACTIVE
                is MentorEvent.ErrorOccurred -> ConversationState.ERROR
                else -> currentState
            }
            ConversationState.STREAMING_RESPONSE -> when (event) {
                MentorEvent.StreamCompleted -> ConversationState.ACTIVE
                MentorEvent.UserCancelled -> ConversationState.ACTIVE
                is MentorEvent.ErrorOccurred -> ConversationState.ERROR
                else -> currentState
            }
            ConversationState.REFLECTION_DRAFTED -> when (event) {
                MentorEvent.ApproveReflection -> ConversationState.COMPLETED
                MentorEvent.UserSentMessage -> ConversationState.ACTIVE
                else -> currentState
            }
            ConversationState.ERROR -> when (event) {
                MentorEvent.UserSentMessage -> ConversationState.WAITING_FOR_PROVIDER
                else -> currentState
            }
            ConversationState.COMPLETED, ConversationState.READY_FOR_REFLECTION, ConversationState.AWAITING_APPROVAL, ConversationState.ARCHIVED -> currentState
        }
        return currentState
    }
}
