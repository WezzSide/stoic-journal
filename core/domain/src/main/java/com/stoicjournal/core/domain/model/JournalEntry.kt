package com.stoicjournal.core.domain.model

import java.time.Instant

data class JournalEntry(
    val id: String,
    val type: EntryType,
    val promptId: String? = null,
    val title: String? = null,
    val body: String,
    val quoteId: String? = null,
    val exerciseId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
