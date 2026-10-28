package com.stoicjournal.core.domain.model

import java.time.Instant

data class MentorReflectionLink(
    val id: String,
    val conversationId: String,
    val candidateId: String,
    val journalEntryId: String,
    val createdAt: Instant,
    val savePolicy: String
)
