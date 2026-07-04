package com.stoicjournal.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.core.domain.model.Theme
import com.stoicjournal.feature.library.components.PhilosopherList
import com.stoicjournal.feature.library.components.QuoteCard
import com.stoicjournal.feature.library.components.ThemeChip
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onQuoteClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredQuotes by viewModel.filteredQuotes.collectAsState()
    val tabs = listOf("Themes", "Philosophers", "Favorites")
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                selectedTab = page
                viewModel.selectFilter(filterForTab(page, viewModel.uiState.value.selectedFilter))
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wisdom Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(title) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> ThemesTab(
                        selectedFilter = uiState.selectedFilter,
                        onFilter = viewModel::selectFilter,
                        quotes = filteredQuotes,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onQuoteClick = onQuoteClick
                    )
                    1 -> PhilosophersTab(
                        philosophers = uiState.philosophers,
                        selectedFilter = uiState.selectedFilter,
                        onFilter = viewModel::selectFilter,
                        quotes = filteredQuotes,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onQuoteClick = onQuoteClick
                    )
                    2 -> QuotesList(
                        quotes = filteredQuotes,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onQuoteClick = onQuoteClick
                    )
                }
            }
        }
    }
}

private fun filterForTab(
    tab: Int,
    currentFilter: LibraryFilter
): LibraryFilter = when (tab) {
    2 -> LibraryFilter.Favorites
    1 -> if (currentFilter is LibraryFilter.ByPhilosopher) {
        currentFilter
    } else {
        LibraryFilter.All
    }
    else -> if (currentFilter is LibraryFilter.ByTheme) {
        currentFilter
    } else {
        LibraryFilter.All
    }
}

@Composable
private fun ThemesTab(
    selectedFilter: LibraryFilter,
    onFilter: (LibraryFilter) -> Unit,
    quotes: List<com.stoicjournal.core.domain.model.Quote>,
    onToggleFavorite: (String) -> Unit,
    onQuoteClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(Theme.entries) { theme ->
                ThemeChip(
                    theme = theme,
                    selected = selectedFilter == LibraryFilter.ByTheme(theme),
                    onClick = { onFilter(LibraryFilter.ByTheme(theme)) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        QuotesList(quotes = quotes, onToggleFavorite = onToggleFavorite, onQuoteClick = onQuoteClick)
    }
}

@Composable
private fun PhilosophersTab(
    philosophers: List<com.stoicjournal.core.domain.model.Philosopher>,
    selectedFilter: LibraryFilter,
    onFilter: (LibraryFilter) -> Unit,
    quotes: List<com.stoicjournal.core.domain.model.Quote>,
    onToggleFavorite: (String) -> Unit,
    onQuoteClick: (String) -> Unit
) {
    val selectedId = (selectedFilter as? LibraryFilter.ByPhilosopher)?.philosopherId
    Column(modifier = Modifier.fillMaxWidth()) {
        PhilosopherList(
            philosophers = philosophers,
            selectedId = selectedId,
            onSelect = { onFilter(LibraryFilter.ByPhilosopher(it)) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        QuotesList(quotes = quotes, onToggleFavorite = onToggleFavorite, onQuoteClick = onQuoteClick)
    }
}

@Composable
private fun QuotesList(
    quotes: List<com.stoicjournal.core.domain.model.Quote>,
    onToggleFavorite: (String) -> Unit,
    onQuoteClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        items(quotes, key = { it.id }) { quote ->
            QuoteCard(
                quote = quote,
                onToggleFavorite = { onToggleFavorite(quote.id) },
                onClick = { onQuoteClick(quote.id) }
            )
        }
    }
}
