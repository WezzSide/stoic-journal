package com.stoicjournal.feature.settings.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stoicjournal.core.domain.model.MentorMemory
import com.stoicjournal.core.domain.model.MentorMemoryStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MemorySettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    var newMemoryText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory & Context") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.error?.let { message ->
                item {
                    Snackbar(action = { TextButton(onClick = viewModel::clearError) { Text("Dismiss") } }) { Text(message) }
                }
            }
            item {
                Text(
                    "Stoic Mentor only remembers what you approve here. Nothing from your journal is sent to an AI provider unless every switch in the chain below is on.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ToggleRow(
                    title = "Enable contextual memory",
                    subtitle = "Master switch. Off = Mentor sees only the current conversation.",
                    checked = prefs.memoryEnabled,
                    tag = "MemoryEnabledSwitch",
                    onCheckedChange = viewModel::setMemoryEnabled
                )
            }
            item {
                ToggleRow(
                    title = "Use approved memories",
                    subtitle = "Include the approved items listed below as context.",
                    checked = prefs.useApprovedMemories,
                    enabled = prefs.memoryEnabled,
                    tag = "UseApprovedMemoriesSwitch",
                    onCheckedChange = viewModel::setUseApprovedMemories
                )
            }
            item {
                ToggleRow(
                    title = "Allow access to past journal entries",
                    subtitle = "Lets Mentor look up a few related past reflections (first 150 characters each).",
                    checked = prefs.historicalJournalAccessEnabled,
                    enabled = prefs.memoryEnabled,
                    tag = "HistoricalAccessSwitch",
                    onCheckedChange = viewModel::setHistoricalJournalAccessEnabled
                )
            }
            item {
                ToggleRow(
                    title = "Send historical context to AI provider",
                    subtitle = "Required consent. When off, memories and past entries stay on this device even if the switches above are on.",
                    checked = prefs.sendHistoricalContextToProvider,
                    enabled = prefs.memoryEnabled,
                    tag = "SendContextSwitch",
                    onCheckedChange = viewModel::setSendHistoricalContextToProvider
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                Text("Memories", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Add something you want Mentor to keep in mind, then approve it. Deleting a memory never deletes the journal entries it came from.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                OutlinedTextField(
                    value = newMemoryText,
                    onValueChange = { newMemoryText = it },
                    label = { Text("New memory") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("NewMemoryField"),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        viewModel.proposeMemory(newMemoryText)
                        newMemoryText = ""
                    },
                    enabled = newMemoryText.isNotBlank(),
                    modifier = Modifier.testTag("ProposeMemoryButton")
                ) { Text("Add as proposal") }
            }
            if (uiState.memories.isEmpty()) {
                item {
                    Text(
                        "No memories yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(uiState.memories, key = { it.id }) { memory ->
                MemoryCard(
                    memory = memory,
                    onApprove = { viewModel.approveMemory(memory.id) },
                    onDelete = { viewModel.deleteMemory(memory.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun MemoryCard(
    memory: MentorMemory,
    onApprove: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(memory.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {},
                    label = { Text(memory.status.name.lowercase().replace('_', ' ')) }
                )
                Spacer(modifier = Modifier.weight(1f))
                if (memory.status != MentorMemoryStatus.APPROVED) {
                    TextButton(
                        onClick = onApprove,
                        modifier = Modifier.testTag("ApproveMemory_${memory.id}")
                    ) { Text("Approve") }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("DeleteMemory_${memory.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete memory", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
