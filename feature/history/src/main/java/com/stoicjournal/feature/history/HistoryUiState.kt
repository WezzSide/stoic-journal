package com.stoicjournal.feature.history

import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry

data class HistoryUiState(
    val entries: List<JournalEntry> = emptyList(),
    val selectedFilter: EntryType? = null,
    val isLoading: Boolean = true
)
