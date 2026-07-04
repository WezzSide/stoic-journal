package com.stoicjournal.feature.journal.exercise

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.feature.journal.components.JournalTextField
import com.stoicjournal.feature.journal.components.SaveDiscardBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseJournalScreen(
    exerciseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExerciseJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(uiState.exercise?.title ?: "Exercise") }) },
        bottomBar = {
            SaveDiscardBar(
                onSave = { viewModel.save(onNavigateBack) },
                onDiscard = onNavigateBack,
                saveEnabled = uiState.reflection.isNotBlank(),
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
            uiState.exercise?.let { exercise ->
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                exercise.steps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            Text(
                text = "Reflection",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            JournalTextField(
                value = uiState.reflection,
                onValueChange = viewModel::updateReflection,
                placeholder = {
                    Text(
                        "Write your reflection...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
