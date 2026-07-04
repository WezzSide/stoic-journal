package com.stoicjournal.feature.library

import com.stoicjournal.core.domain.model.Philosopher
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Theme

sealed class LibraryFilter {
    data object All : LibraryFilter()
    data object Favorites : LibraryFilter()
    data class ByPhilosopher(val philosopherId: String) : LibraryFilter()
    data class ByTheme(val theme: Theme) : LibraryFilter()
}

data class LibraryUiState(
    val philosophers: List<Philosopher> = emptyList(),
    val quotes: List<Quote> = emptyList(),
    val selectedFilter: LibraryFilter = LibraryFilter.All,
    val isLoading: Boolean = true,
    val error: String? = null
)
