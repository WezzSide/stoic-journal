package com.stoicjournal.feature.journal.freeform

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.feature.journal.components.JournalTextField
import com.stoicjournal.feature.journal.components.QuoteHeader
import com.stoicjournal.feature.journal.components.SaveDiscardBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeFormJournalScreen(
    onNavigateBack: () -> Unit,
    viewModel: FreeFormJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Free-form Entry") }) },
        bottomBar = {
            SaveDiscardBar(
                onSave = { viewModel.save(onNavigateBack) },
                onDiscard = onNavigateBack,
                saveEnabled = uiState.body.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Title (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            QuoteHeader(quote = uiState.attachedQuote)
            if (uiState.attachedQuote == null) {
                TextButton(onClick = viewModel::attachRandomQuote) {
                    Text("Add a Stoic quote")
                }
            } else {
                TextButton(onClick = viewModel::removeQuote) {
                    Text("Remove quote")
                }
            }
            JournalTextField(
                value = uiState.body,
                onValueChange = viewModel::updateBody,
                placeholder = {
                    Text(
                        "Write freely...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
