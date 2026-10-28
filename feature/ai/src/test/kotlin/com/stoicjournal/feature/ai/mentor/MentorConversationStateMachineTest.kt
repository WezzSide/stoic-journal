package com.stoicjournal.feature.ai.mentor

import com.stoicjournal.core.domain.model.ConversationState
import com.stoicjournal.core.domain.model.MentorConversationStateMachine
import com.stoicjournal.core.domain.model.MentorEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class MentorConversationStateMachineTest {

    @Test
    fun `initial state is CREATED`() {
        val stateMachine = MentorConversationStateMachine()
        assertEquals(ConversationState.CREATED, stateMachine.currentState)
    }

    @Test
    fun `transition from CREATED to WAITING_FOR_PROVIDER when user sends message`() {
        val stateMachine = MentorConversationStateMachine()
        val newState = stateMachine.transition(MentorEvent.UserSentMessage)
        assertEquals(ConversationState.WAITING_FOR_PROVIDER, newState)
    }

    @Test
    fun `transition from WAITING_FOR_PROVIDER to STREAMING_RESPONSE when streaming starts`() {
        val stateMachine = MentorConversationStateMachine(ConversationState.WAITING_FOR_PROVIDER)
        val newState = stateMachine.transition(MentorEvent.StartStreaming)
        assertEquals(ConversationState.STREAMING_RESPONSE, newState)
    }

    @Test
    fun `transition from STREAMING_RESPONSE to ACTIVE on completion`() {
        val stateMachine = MentorConversationStateMachine(ConversationState.STREAMING_RESPONSE)
        val newState = stateMachine.transition(MentorEvent.StreamCompleted)
        assertEquals(ConversationState.ACTIVE, newState)
    }

    @Test
    fun `transition from STREAMING_RESPONSE to ACTIVE on cancellation`() {
        val stateMachine = MentorConversationStateMachine(ConversationState.STREAMING_RESPONSE)
        val newState = stateMachine.transition(MentorEvent.UserCancelled)
        assertEquals(ConversationState.ACTIVE, newState)
    }
}
