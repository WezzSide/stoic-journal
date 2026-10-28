package com.stoicjournal.feature.ai.mentor

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stoicjournal.core.domain.model.MentorMessage
import com.stoicjournal.core.domain.model.MessageRole
import com.stoicjournal.feature.ai.mentor.components.ChatDayHeader
import com.stoicjournal.feature.ai.mentor.components.MentorAvatar
import com.stoicjournal.feature.ai.mentor.components.MentorComposer
import com.stoicjournal.feature.ai.mentor.components.MentorEmptyState
import com.stoicjournal.feature.ai.mentor.components.MentorMessageBubble
import com.stoicjournal.feature.ai.mentor.components.MentorTypingIndicator
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun MentorChatScreen(
    conversationId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: (() -> Unit)? = null,
    viewModel: MentorChatViewModel = hiltViewModel()
) {
    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }
    // Returning from Settings (e.g. after adding an API key) must show the newly selected provider.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshProvider() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var draft by rememberSaveable { mutableStateOf("") }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            // Dictated text goes into the input field so the user can review it before sending.
            if (!spoken.isNullOrBlank()) draft = if (draft.isBlank()) spoken else "$draft $spoken"
        }
    }

    MentorChatContent(
        uiState = uiState,
        draft = draft,
        onDraftChange = { draft = it },
        onSend = {
            val text = draft.trim()
            if (text.isNotEmpty() && !uiState.isStreaming) {
                viewModel.sendMessage(text)
                draft = ""
            }
        },
        onVoiceClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Stoic Mentor…")
            }
            try {
                speechRecognizerLauncher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                // No speech recogniser installed (e.g. some emulators): nothing to do, typing still works.
            }
        },
        onCancel = viewModel::cancelActiveRequest,
        onStarterSelected = { starter ->
            viewModel.setMode(starter.mode)
            viewModel.sendMessage("Let's begin: ${starter.label}")
        },
        onGenerateReflection = { viewModel.generateReflection(onCandidateGenerated = onNavigateToPreview) },
        onDismissError = viewModel::clearError,
        onOpenSettings = onOpenSettings,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/** Stateless chat UI (previewable). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentorChatContent(
    uiState: MentorChatUiState,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    onCancel: () -> Unit,
    onStarterSelected: (com.stoicjournal.feature.ai.mentor.components.MentorStarter) -> Unit,
    onGenerateReflection: () -> Unit,
    onDismissError: () -> Unit,
    onOpenSettings: (() -> Unit)?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    // Chronological rows, rendered reversed in a reverse-layout list so the newest message sits right
    // above the input bar and the list stays anchored to the bottom as messages arrive.
    val items = remember(uiState.messages, uiState.isStreaming, uiState.streamingText) {
        buildChatItems(uiState.messages, uiState.isStreaming, uiState.streamingText).asReversed()
    }
    val today = remember { LocalDate.now() }

    // Jump to the newest message when the user sends one or a reply starts — but don't yank the list
    // away if the user scrolled up to read older messages while a reply streams in.
    val lastMessage = uiState.messages.lastOrNull()
    LaunchedEffect(uiState.messages.size, uiState.isStreaming) {
        val userJustSent = lastMessage?.role == MessageRole.USER
        if (userJustSent || listState.firstVisibleItemIndex <= 2) listState.animateScrollToItem(0)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MentorAvatar(size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Stoic Mentor", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (uiState.isStreaming) "typing…" else "via ${providerDisplayName(uiState.activeProviderId)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.isStreaming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                // Keep the input bar above the gesture/navigation bar and above the keyboard.
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom))
        ) {
            if (uiState.isEmptyConversation) {
                MentorEmptyState(
                    onStarterSelected = onStarterSelected,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    items(items, key = { it.key }, contentType = { it::class }) { item ->
                        when (item) {
                            is ChatItem.DayHeader -> ChatDayHeader(dayLabel(item.date, today))
                            is ChatItem.Message -> MentorMessageBubble(
                                message = item.message,
                                isLastInGroup = item.isLastInGroup,
                                modifier = Modifier.padding(top = if (item.isFirstInGroup) 8.dp else 2.dp)
                            )
                            ChatItem.Typing -> MentorTypingIndicator(Modifier.padding(top = 8.dp))
                            is ChatItem.Streaming -> MentorMessageBubble(
                                message = MentorMessage(
                                    id = "streaming",
                                    conversationId = uiState.conversation?.id.orEmpty(),
                                    role = MessageRole.MENTOR,
                                    content = item.text,
                                    timestamp = Instant.now(),
                                    sequenceNumber = Int.MAX_VALUE
                                ),
                                isStreaming = true,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = uiState.error != null) {
                uiState.error?.let { error ->
                    ChatErrorBanner(error, onDismissError, onOpenSettings.takeIf { error.opensSettings })
                }
            }

            AnimatedVisibility(visible = uiState.canGenerateReflection || uiState.isGeneratingCandidate) {
                ReflectionBar(isGenerating = uiState.isGeneratingCandidate, onGenerate = onGenerateReflection)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MentorComposer(
                text = draft,
                onTextChange = onDraftChange,
                onSend = onSend,
                onVoiceClick = onVoiceClick,
                onCancelClick = onCancel,
                isStreaming = uiState.isStreaming
            )
        }
    }
}

@Composable
private fun ChatErrorBanner(error: MentorChatError, onDismiss: () -> Unit, onOpenSettings: (() -> Unit)?) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(error.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(error.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (onOpenSettings != null) {
                    TextButton(onClick = onOpenSettings) { Text("Open Settings") }
                }
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}

@Composable
private fun ReflectionBar(isGenerating: Boolean, onGenerate: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, end = 4.dp)) {
            Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Done? Save the key insights to your journal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onGenerate, enabled = !isGenerating) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Generating…")
                } else {
                    Text("Generate Reflection")
                }
            }
        }
    }
}

// ---- Previews ---------------------------------------------------------------------------------

private fun previewMessages(): List<MentorMessage> {
    val now = Instant.now()
    fun m(seq: Int, role: MessageRole, text: String, minutesAgo: Long) =
        MentorMessage("p$seq", "p", role, text, now.minus(minutesAgo, ChronoUnit.MINUTES), seq)
    return listOf(
        m(1, MessageRole.USER, "Today I lost my temper in traffic and I keep replaying it.", 12),
        m(2, MessageRole.MENTOR, "That sounds frustrating. Which part of that moment was truly within your control — the traffic, or your response to it?", 11),
        m(3, MessageRole.USER, "My response, I guess.", 9),
        m(4, MessageRole.USER, "But it felt automatic.", 9)
    )
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun MentorChatConversationPreview() {
    MaterialTheme {
        MentorChatContent(
            uiState = MentorChatUiState(messages = previewMessages(), isStreaming = true, streamingText = "Automatic reactions are where practice"),
            draft = "",
            onDraftChange = {}, onSend = {}, onVoiceClick = {}, onCancel = {}, onStarterSelected = {},
            onGenerateReflection = {}, onDismissError = {}, onOpenSettings = {}, onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun MentorChatErrorPreview() {
    MaterialTheme {
        MentorChatContent(
            uiState = MentorChatUiState(
                messages = previewMessages().take(2),
                error = com.stoicjournal.core.domain.model.AiError.MissingCredentials.toChatError("openai")
            ),
            draft = "How do I practise this?",
            onDraftChange = {}, onSend = {}, onVoiceClick = {}, onCancel = {}, onStarterSelected = {},
            onGenerateReflection = {}, onDismissError = {}, onOpenSettings = {}, onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun MentorChatEmptyPreview() {
    MaterialTheme {
        MentorChatContent(
            uiState = MentorChatUiState(),
            draft = "",
            onDraftChange = {}, onSend = {}, onVoiceClick = {}, onCancel = {}, onStarterSelected = {},
            onGenerateReflection = {}, onDismissError = {}, onOpenSettings = {}, onNavigateBack = {}
        )
    }
}
