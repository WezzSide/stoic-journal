package com.stoicjournal.feature.journal.exercise

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val snackbarHostState = remember { SnackbarHostState() }

    var insideControlText by remember { mutableStateOf("") }
    var outsideControlText by remember { mutableStateOf("") }

    val isDichotomy = uiState.exercise?.title?.contains("Control", ignoreCase = true) == true ||
            uiState.exercise?.title?.contains("Controle", ignoreCase = true) == true

    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onSaveErrorShown()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(uiState.exercise?.title ?: "Exercise") }) },
        bottomBar = {
            SaveDiscardBar(
                onSave = { viewModel.save(onNavigateBack) },
                onDiscard = { viewModel.discard(onNavigateBack) },
                saveEnabled = uiState.reflection.isNotBlank(),
                isSaving = uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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

            if (isDichotomy) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Cirkel van Invloed Opsplitsing ⚖️",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = insideControlText,
                    onValueChange = {
                        insideControlText = it
                        val formatted = buildDichotomyReflection(insideControlText, outsideControlText)
                        viewModel.updateReflection(formatted)
                    },
                    label = { Text("🟢 Binnen mijn controle (Mijn acties, gedachten, keuzes)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = outsideControlText,
                    onValueChange = {
                        outsideControlText = it
                        val formatted = buildDichotomyReflection(insideControlText, outsideControlText)
                        viewModel.updateReflection(formatted)
                    },
                    label = { Text("🔴 Buiten mijn controle (Gebeurtenissen, mening van anderen)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            } else {
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
}

private fun buildDichotomyReflection(inside: String, outside: String): String {
    val builder = StringBuilder()
    if (inside.isNotBlank()) {
        builder.append("### 🟢 Binnen mijn controle:\n").append(inside.trim()).append("\n\n")
    }
    if (outside.isNotBlank()) {
        builder.append("### 🔴 Buiten mijn controle:\n").append(outside.trim())
    }
    return builder.toString().trim()
}
