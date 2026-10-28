package com.stoicjournal.feature.ai.mentor

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.core.domain.model.MentorMessage
import com.stoicjournal.core.domain.model.MessageRole
import com.stoicjournal.feature.ai.mentor.components.MentorComposer
import com.stoicjournal.feature.ai.mentor.components.MentorMessageBubble
import com.stoicjournal.feature.ai.mentor.components.MentorSuggestedPrompts
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentorChatScreen(
    conversationId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    viewModel: MentorChatViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.sendMessage(spokenText)
            }
        }
    }

    val launchSpeechRecognizer = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Stoic Mentor...")
        }
        speechRecognizerLauncher.launch(intent)
    }

    LaunchedEffect(uiState.messages.size, uiState.streamingText) {
        if (uiState.messages.isNotEmpty() || uiState.streamingText.isNotEmpty()) {
            listState.animateScrollToItem((uiState.messages.size + if (uiState.streamingText.isNotEmpty()) 1 else 0))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Stoic Mentor",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Provider: ${uiState.activeProviderId.uppercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column {
                MentorSuggestedPrompts(
                    onSelectMode = { label, mode ->
                        viewModel.setMode(mode)
                        viewModel.sendMessage("Let's begin: $label")
                    }
                )
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                    if (uiState.messages.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.generateReflection(onCandidateGenerated = onNavigateToPreview) },
                            enabled = !uiState.isStreaming && !uiState.isGeneratingCandidate,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            if (uiState.isGeneratingCandidate) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generating...")
                            } else {
                                Text("Generate Reflection")
                            }
                        }
                    }
                }
                MentorComposer(
                    onSendMessage = { content -> viewModel.sendMessage(content) },
                    onVoiceClick = { launchSpeechRecognizer() },
                    onCancelClick = { viewModel.cancelActiveRequest() },
                    isStreaming = uiState.isStreaming
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            uiState.errorMessage?.let { errorMsg ->
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(errorMsg)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.messages.isEmpty() && uiState.streamingText.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Welcome to Stoic Mentor.\n\nType or speak a message to begin reflecting on your day.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(
                        items = uiState.messages,
                        key = { it.id }
                    ) { message ->
                        MentorMessageBubble(message = message)
                    }

                    if (uiState.streamingText.isNotEmpty()) {
                        item {
                            MentorMessageBubble(
                                message = MentorMessage(
                                    id = "streaming_temp",
                                    conversationId = uiState.conversation?.id ?: "",
                                    role = MessageRole.MENTOR,
                                    content = uiState.streamingText,
                                    timestamp = Instant.now(),
                                    sequenceNumber = uiState.messages.size + 1
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
