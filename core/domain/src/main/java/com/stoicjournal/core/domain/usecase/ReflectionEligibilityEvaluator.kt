package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.MentorMessage
import com.stoicjournal.core.domain.model.MessageRole
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ReflectionEligibility {
    data object NotEligible : ReflectionEligibility
    data object NeedsMoreContext : ReflectionEligibility
    data object EligibleForDraft : ReflectionEligibility
    data object ExplicitlyRequested : ReflectionEligibility
}

@Singleton
class ReflectionEligibilityEvaluator @Inject constructor() {

    fun evaluate(messages: List<MentorMessage>, isExplicitRequest: Boolean = false): ReflectionEligibility {
        if (isExplicitRequest) return ReflectionEligibility.ExplicitlyRequested

        val userMessages = messages.filter { it.role == MessageRole.USER }
        if (userMessages.isEmpty()) return ReflectionEligibility.NotEligible

        val totalUserLength = userMessages.sumOf { it.content.length }
        if (totalUserLength < 25) return ReflectionEligibility.NeedsMoreContext

        val reflectionKeywords = listOf(
            "today", "felt", "control", "learned", "struggled", "virtue",
            "decision", "stoic", "reflection", "vandaag", "voelde", "geleerd"
        )

        val hasReflectionContent = userMessages.any { msg ->
            reflectionKeywords.any { keyword -> msg.content.contains(keyword, ignoreCase = true) }
        }

        return if (hasReflectionContent || userMessages.size >= 2) {
            ReflectionEligibility.EligibleForDraft
        } else {
            ReflectionEligibility.NeedsMoreContext
        }
    }
}
