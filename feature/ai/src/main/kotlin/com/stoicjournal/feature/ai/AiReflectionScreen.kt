package com.stoicjournal.feature.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.core.common.components.TypewriterText
import com.stoicjournal.core.domain.model.AiReflectionResponse
import com.stoicjournal.core.domain.model.AiReflectionTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiReflectionScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiReflectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    AiReflectionContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onGetReflection = viewModel::getReflection,
        onSaveToJournal = viewModel::saveToJournal
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AiReflectionContent(
    uiState: AiReflectionUiState,
    onNavigateBack: () -> Unit,
    onGetReflection: (String, AiReflectionTheme) -> Unit,
    onSaveToJournal: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf(AiReflectionTheme.FOCUS) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stoic Reflection") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Back" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "What is on your mind?",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Start)
            )

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Describe your thought or concern...") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Reflection input field" },
                minLines = 4,
                placeholder = { Text("e.g., I'm feeling overwhelmed by my workload.") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (inputText.isNotBlank()) {
                        onGetReflection(inputText, selectedTheme)
                    }
                })
            )

            Text(
                text = "Choose a theme",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Theme selection chips" },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiReflectionTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = selectedTheme == theme,
                        onClick = { selectedTheme = theme },
                        label = { Text(theme.displayName) },
                        modifier = Modifier.semantics { contentDescription = "Select ${theme.displayName} theme" }
                    )
                }
            }

            Button(
                onClick = { 
                    focusManager.clearFocus()
                    onGetReflection(inputText, selectedTheme) 
                },
                enabled = inputText.isNotBlank() && uiState !is AiReflectionUiState.Loading,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Generate reflection button" }
            ) {
                Text(if (uiState is AiReflectionUiState.Success) "Regenerate Reflection" else "Get Stoic Reflection")
            }

            when (val state = uiState) {
                is AiReflectionUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(32.dp).semantics { contentDescription = "Loading indicator" }
                    )
                    Text("The philosopher is contemplating...", style = MaterialTheme.typography.bodyMedium)
                }
                is AiReflectionUiState.Success -> {
                    ReflectionResultCard(
                        response = state.reflection,
                        isSaved = state.isSaved,
                        onSave = onSaveToJournal
                    )
                }
                is AiReflectionUiState.Error -> {
                    ErrorMessage(
                        message = state.message,
                        onRetry = { onGetReflection(inputText, selectedTheme) }
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ReflectionResultCard(
    response: AiReflectionResponse,
    isSaved: Boolean,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Reflection result card" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = response.insight.isNotEmpty(),
                enter = fadeIn() + expandVertically()
            ) {
                Column {
                    Text(text = "Insight", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    TypewriterText(text = response.insight, style = MaterialTheme.typography.bodyLarge)
                }
            }

            AnimatedVisibility(
                visible = response.interpretation.isNotEmpty(),
                enter = fadeIn() + expandVertically()
            ) {
                Column {
                    Text(text = "Interpretation", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    TypewriterText(text = response.interpretation, style = MaterialTheme.typography.bodyMedium)
                }
            }

            AnimatedVisibility(
                visible = response.reflectiveQuestion.isNotEmpty(),
                enter = fadeIn() + expandVertically()
            ) {
                Column {
                    Text(text = "Reflective Question", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(text = response.reflectiveQuestion, style = MaterialTheme.typography.bodyMedium)
                }
            }

            AnimatedVisibility(
                visible = response.actionableStep.isNotEmpty(),
                enter = fadeIn() + expandVertically()
            ) {
                Column {
                    Text(text = "Actionable Step", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(text = response.actionableStep, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (response.quote != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(text = response.quote!!, style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                if (response.principle != null) {
                    Text(text = "Principle: ${response.principle}", style = MaterialTheme.typography.labelSmall)
                }
            }

            Button(
                onClick = onSave,
                enabled = !isSaved,
                modifier = Modifier.align(Alignment.End).semantics { contentDescription = if (isSaved) "Reflection saved" else "Save reflection to journal" },
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isSaved) "Saved to Journal" else "Save to Journal")
            }
        }
    }
}

@Composable
fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics { contentDescription = "Error message: $message" }
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        TextButton(
            onClick = onRetry,
            modifier = Modifier.semantics { contentDescription = "Retry generating reflection" }
        ) {
            Text("Retry")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AiReflectionContentPreview_Idle() {
    AiReflectionContent(
        uiState = AiReflectionUiState.Idle,
        onNavigateBack = {},
        onGetReflection = { _, _ -> },
        onSaveToJournal = {}
    )
}

@Preview(showBackground = true)
@Composable
fun AiReflectionContentPreview_Loading() {
    AiReflectionContent(
        uiState = AiReflectionUiState.Loading,
        onNavigateBack = {},
        onGetReflection = { _, _ -> },
        onSaveToJournal = {}
    )
}

@Preview(showBackground = true)
@Composable
fun AiReflectionContentPreview_Success() {
    AiReflectionContent(
        uiState = AiReflectionUiState.Success(
            reflection = AiReflectionResponse(
                insight = "Suffer not in imagination.",
                interpretation = "Focus on what you can control right now.",
                reflectiveQuestion = "What is truly within your power?",
                actionableStep = "Write down one thing you will stop worrying about.",
                quote = "We suffer more in imagination than in reality.",
                principle = "Dichotomy of Control",
                theme = AiReflectionTheme.ANXIETY
            ),
            isSaved = false
        ),
        onNavigateBack = {},
        onGetReflection = { _, _ -> },
        onSaveToJournal = {}
    )
}
