package com.stoicjournal.core.domain.model

import java.time.Instant

data class ReflectionCandidate(
    val candidateId: String,
    val conversationId: String,
    val title: String,
    val summary: String,
    val whatHappened: String,
    val keyInsights: String,
    val withinControl: String,
    val outsideControl: String,
    val virtues: List<Virtue>,
    val nextAction: String,
    val generatedAt: Instant = Instant.now(),
    val providerId: String,
    val modelId: String
)
