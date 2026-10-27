package com.stoicjournal.core.domain.model

import java.time.LocalDateTime

data class AiReflectionRequest(
    val content: String,
    val theme: AiReflectionTheme = AiReflectionTheme.FOCUS,
    val mood: String? = null,
    val journalContext: String? = null
)

data class AiReflectionResponse(
    val insight: String,
    val interpretation: String,
    val reflectiveQuestion: String,
    val actionableStep: String,
    val quote: String? = null,
    val principle: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val theme: AiReflectionTheme
)

enum class AiReflectionTheme(val displayName: String) {
    ANXIETY("Anxiety"),
    DISCIPLINE("Discipline"),
    ANGER("Anger"),
    GRIEF("Grief"),
    FOCUS("Focus"),
    GRATITUDE("Gratitude"),
    DECISION_MAKING("Decision Making"),
    SELF_CONTROL("Self Control")
}

sealed class AiFailureReason {
    data object MissingApiKey : AiFailureReason()
    data object NetworkError : AiFailureReason()
    data object RateLimit : AiFailureReason()
    data object SafetyBlocked : AiFailureReason()
    data object EmptyResponse : AiFailureReason()
    data class Generic(val message: String) : AiFailureReason()
}
