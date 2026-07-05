package com.stoicjournal.feature.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AiReflectionScreen(
    viewModel: AiReflectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Stoic AI Reflection",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("What's on your mind?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Button(
            onClick = { viewModel.getReflection(inputText) },
            enabled = inputText.isNotBlank() && uiState !is AiReflectionUiState.Loading
        ) {
            Text("Get Reflection")
        }

        when (val state = uiState) {
            is AiReflectionUiState.Loading -> {
                CircularProgressIndicator()
            }
            is AiReflectionUiState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = state.reflection,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            is AiReflectionUiState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }
    }
}
