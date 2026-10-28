package com.stoicjournal.feature.today

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.feature.today.components.DailyPromptCard
import com.stoicjournal.feature.today.components.OnThisDayCard
import com.stoicjournal.feature.today.components.QuickActionsRow
import com.stoicjournal.feature.today.components.StreakIndicator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onGuidedJournal: (String) -> Unit,
    onFreeFormJournal: () -> Unit,
    onExercise: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAiReflection: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result is intentionally ignored; the UI is not blocked if denied.
    }

    LaunchedEffect(uiState.isLoading, uiState.reminderEnabled, uiState.hasRequestedNotificationPermission) {
        if (uiState.isLoading) return@LaunchedEffect
        if (!uiState.reminderEnabled) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED &&
                !uiState.hasRequestedNotificationPermission
            ) {
                notificationPermissionLauncher.launch(permission)
                viewModel.setNotificationPermissionRequested(true)
            }
        }
    }

    TodayScreenContent(
        uiState = uiState,
        onGuidedJournal = onGuidedJournal,
        onFreeFormJournal = onFreeFormJournal,
        onExercise = onExercise,
        onNavigateToLibrary = onNavigateToLibrary,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToAiReflection = onNavigateToAiReflection,
        onLoadErrorShown = viewModel::loadErrorShown
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayScreenContent(
    uiState: TodayUiState,
    onGuidedJournal: (String) -> Unit,
    onFreeFormJournal: () -> Unit,
    onExercise: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAiReflection: () -> Unit,
    onLoadErrorShown: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.loadError) {
        uiState.loadError?.let { message ->
            snackbarHostState.showSnackbar(message)
            onLoadErrorShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Stoic Journal") },
                actions = {
                    IconButton(onClick = onNavigateToAiReflection) {
                        Icon(Icons.Default.Psychology, contentDescription = "AI Reflection")
                    }
                    IconButton(onClick = onNavigateToLibrary) {
                        Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Library")
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StreakIndicator(streak = uiState.streak)

            uiState.onThisDayEntry?.let { pastEntry ->
                OnThisDayCard(
                    entry = pastEntry,
                    onClick = onNavigateToHistory,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            AnimatedVisibility(
                visible = uiState.streakInsight != null,
                enter = fadeIn()
            ) {
                Text(
                    text = uiState.streakInsight ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            uiState.prompt?.let { prompt ->
                DailyPromptCard(
                    prompt = prompt,
                    quote = uiState.quote,
                    onClick = { onGuidedJournal(prompt.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            QuickActionsRow(
                onFreeForm = onFreeFormJournal,
                onExercise = {
                    val exerciseId = uiState.exercises.firstOrNull()?.id ?: "exercise-dichotomy-of-control"
                    onExercise(exerciseId)
                },
                onAiReflection = onNavigateToAiReflection,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.exercises.isNotEmpty()) {
                Text(
                    text = "Stoic Exercises",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                uiState.exercises.forEach { exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.title) },
                        supportingContent = { Text(exercise.description) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExercise(exercise.id) },
                        trailingContent = {
                            IconButton(onClick = { onExercise(exercise.id) }) {
                                Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Start exercise")
                            }
                        }
                    )
                }
            }
        }
    }
}
