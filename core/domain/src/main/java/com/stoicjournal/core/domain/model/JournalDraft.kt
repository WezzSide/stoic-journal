package com.stoicjournal.core.domain.model

/**
 * Lightweight, transient draft of an in-progress journal entry.
 * Only the fields relevant to the entry type are populated.
 */
data class JournalDraft(
    val title: String = "",
    val body: String = "",
    val reflection: String = "",
    val quoteId: String? = null
)
