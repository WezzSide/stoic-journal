package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.MentorMessage
import com.stoicjournal.core.domain.model.MessageRole
import com.stoicjournal.core.domain.model.ReflectionCandidate
import com.stoicjournal.core.domain.model.Virtue
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectionAutomationTest {

    private val evaluator = ReflectionEligibilityEvaluator()
    private val validator = ReflectionCandidateValidator()

    @Test
    fun `evaluate returns NotEligible for empty messages`() {
        val result = evaluator.evaluate(emptyList())
        assertEquals(ReflectionEligibility.NotEligible, result)
    }

    @Test
    fun `evaluate returns EligibleForDraft for meaningful user message`() {
        val messages = listOf(
            MentorMessage("1", "conv1", MessageRole.USER, "Today I struggled with remaining calm when receiving criticism.", Instant.now(), 1)
        )
        val result = evaluator.evaluate(messages)
        assertEquals(ReflectionEligibility.EligibleForDraft, result)
    }

    @Test
    fun `validate accepts valid reflection candidate`() {
        val candidate = ReflectionCandidate(
            candidateId = "cand1",
            conversationId = "conv1",
            title = "A Stoic Day",
            summary = "Summary text",
            whatHappened = "Event text",
            keyInsights = "Insights text",
            withinControl = "Controllable",
            outsideControl = "Uncontrollable",
            virtues = listOf(Virtue.WISDOM),
            nextAction = "Action",
            providerId = "openai",
            modelId = "gpt-4o"
        )
        val result = validator.validate(candidate)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validate fails for blank title`() {
        val candidate = ReflectionCandidate(
            candidateId = "cand1",
            conversationId = "conv1",
            title = "   ",
            summary = "Summary text",
            whatHappened = "Event text",
            keyInsights = "Insights text",
            withinControl = "Controllable",
            outsideControl = "Uncontrollable",
            virtues = listOf(Virtue.WISDOM),
            nextAction = "Action",
            providerId = "openai",
            modelId = "gpt-4o"
        )
        val result = validator.validate(candidate)
        assertTrue(result.isFailure)
    }
}
