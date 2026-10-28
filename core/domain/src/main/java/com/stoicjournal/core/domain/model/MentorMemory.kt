package com.stoicjournal.core.domain.model

import java.time.Instant

enum class MentorMemoryCategory {
    USER_PREFERENCE,
    PHILOSOPHICAL_INTEREST,
    USER_APPROVED_GOAL,
    REFLECTION_THEME,
    USER_AUTHORED_INSIGHT
}

enum class MentorMemoryStatus {
    PROPOSED,
    AWAITING_APPROVAL,
    APPROVED,
    REJECTED,
    ARCHIVED,
    DELETED
}

data class MentorMemory(
    val id: String,
    val category: MentorMemoryCategory,
    val content: String,
    val sourceEntryIds: List<String> = emptyList(),
    val sourceConversationIds: List<String> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val lastReviewedAt: Instant? = null,
    val status: MentorMemoryStatus = MentorMemoryStatus.APPROVED,
    val userApproved: Boolean = true
)
