package com.stoicjournal.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.feature.history.components.EntryFilterChips
import com.stoicjournal.feature.history.components.EntryListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onEntryClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchActive by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.loadError) {
        uiState.loadError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.loadErrorShown()
        }
    }

    LaunchedEffect(uiState.deleteError) {
        uiState.deleteError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.deleteErrorShown()
        }
    }

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            snackbarHostState.showSnackbar("Entry deleted")
            viewModel.deleteSuccessShown()
        }
    }

    Scaffold(
        topBar = {
            if (!searchActive) {
                TopAppBar(
                    title = { Text("History") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        val context = androidx.compose.ui.platform.LocalContext.current
                        IconButton(onClick = { 
                            val markdown = viewModel.getMarkdownExport()
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, markdown)
                                type = "text/markdown"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Export Journal")
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Export Markdown")
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (searchActive) 0.dp else padding.calculateTopPadding())
        ) {
            if (searchActive) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    onSearch = { searchActive = false },
                    active = searchActive,
                    onActiveChange = { searchActive = it },
                    placeholder = { Text("Search entries...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { 
                            if (uiState.searchQuery.isEmpty()) {
                                searchActive = false
                            } else {
                                viewModel.updateSearchQuery("")
                            }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HistoryList(
                        uiState = uiState,
                        onEntryClick = onEntryClick,
                        onDeleteEntry = viewModel::deleteEntry
                    )
                }
            } else {
                EntryFilterChips(
                    selectedFilter = uiState.selectedFilter,
                    onSelect = viewModel::selectFilter
                )
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    HistoryList(
                        uiState = uiState,
                        onEntryClick = onEntryClick,
                        onDeleteEntry = viewModel::deleteEntry
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryList(
    uiState: HistoryUiState,
    onEntryClick: (String) -> Unit,
    onDeleteEntry: (com.stoicjournal.core.domain.model.JournalEntry) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        items(uiState.entries, key = { it.id }) { entry ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { dismissValue ->
                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                        onDeleteEntry(entry)
                        true
                    } else {
                        false
                    }
                }
            )
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                content = {
                    LaunchedEffect(uiState.failedDeleteEntryId) {
                        if (uiState.failedDeleteEntryId == entry.id) {
                            dismissState.reset()
                        }
                    }
                    EntryListItem(
                        entry = entry,
                        onClick = { onEntryClick(entry.id) }
                    )
                }
            )
        }
    }
}
