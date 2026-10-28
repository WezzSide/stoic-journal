package com.stoicjournal.feature.ai.mentor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.ReflectionCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentorReflectionPreviewScreen(
    candidate: ReflectionCandidate,
    onSaveReflection: (ReflectionCandidate) -> Unit,
    onContinueConversation: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(candidate.title) }
    var summary by remember { mutableStateOf(candidate.summary) }
    var keyInsights by remember { mutableStateOf(candidate.keyInsights) }
    var withinControl by remember { mutableStateOf(candidate.withinControl) }
    var outsideControl by remember { mutableStateOf(candidate.outsideControl) }
    var nextAction by remember { mutableStateOf(candidate.nextAction) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Reflection") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val updatedCandidate = candidate.copy(
                                title = title,
                                summary = summary,
                                keyInsights = keyInsights,
                                withinControl = withinControl,
                                outsideControl = outsideControl,
                                nextAction = nextAction
                            )
                            onSaveReflection(updatedCandidate)
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save Reflection")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onContinueConversation) {
                        Text("Continue Chat")
                    }
                    Button(
                        onClick = {
                            val updatedCandidate = candidate.copy(
                                title = title,
                                summary = summary,
                                keyInsights = keyInsights,
                                withinControl = withinControl,
                                outsideControl = outsideControl,
                                nextAction = nextAction
                            )
                            onSaveReflection(updatedCandidate)
                        }
                    ) {
                        Text("Save to Journal")
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it },
                label = { Text("Summary") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = keyInsights,
                onValueChange = { keyInsights = it },
                label = { Text("Key Insights") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = withinControl,
                onValueChange = { withinControl = it },
                label = { Text("Within My Control") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = outsideControl,
                onValueChange = { outsideControl = it },
                label = { Text("Outside My Control") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = nextAction,
                onValueChange = { nextAction = it },
                label = { Text("Intention & Next Steps") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
