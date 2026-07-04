package com.stoicjournal.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.core.domain.model.StoicismLevel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result is intentionally ignored; onboarding completes regardless.
    }

    val onRequestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            viewModel.setNotificationPermissionRequested(true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        when (uiState.step) {
            0 -> WelcomeStep(
                onNext = viewModel::nextStep,
                modifier = Modifier.weight(1f)
            )
            1 -> LevelStep(
                selectedLevel = uiState.selectedLevel,
                onSelect = viewModel::selectLevel,
                onNext = viewModel::nextStep,
                modifier = Modifier.weight(1f)
            )
            2 -> ReminderStep(
                hour = uiState.reminderHour,
                minute = uiState.reminderMinute,
                onTimeChange = viewModel::setReminderTime,
                onComplete = { viewModel.completeOnboarding(onComplete) },
                onRequestNotificationPermission = onRequestNotificationPermission,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        Text(
            text = "Welcome to Stoic Journal",
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "A quiet space for daily reflection with the Stoics.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
    Button(
        onClick = onNext,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text("Begin")
    }
}

@Composable
private fun LevelStep(
    selectedLevel: StoicismLevel,
    onSelect: (StoicismLevel) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        Text(
            text = "How familiar are you with Stoicism?",
            style = MaterialTheme.typography.headlineMedium
        )
        StoicismLevel.entries.forEach { level ->
            FilterChip(
                selected = selectedLevel == level,
                onClick = { onSelect(level) },
                label = { Text(level.name.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) }) },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .heightIn(min = 48.dp)
            )
        }
    }
    Button(
        onClick = onNext,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text("Continue")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderStep(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
    onComplete: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timePickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        Text(
            text = "When would you like to reflect?",
            style = MaterialTheme.typography.headlineMedium
        )
        TimePicker(
            state = timePickerState,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
    Button(
        onClick = {
            onTimeChange(timePickerState.hour, timePickerState.minute)
            onRequestNotificationPermission()
            onComplete()
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text("Start journaling")
    }
}
