## Task 8: Implement Onboarding feature

**Files:**
- Create: `feature/onboarding/src/main/java/com/stoicjournal/feature/onboarding/OnboardingScreen.kt`
- Create: `feature/onboarding/src/main/java/com/stoicjournal/feature/onboarding/OnboardingViewModel.kt`
- Create: `feature/onboarding/src/main/java/com/stoicjournal/feature/onboarding/OnboardingUiState.kt`
- Create: `feature/onboarding/build.gradle.kts`

**Interfaces:**
- Consumes: `PreferencesRepository`, `ReminderScheduler`.
- Produces: A 3-step onboarding screen that sets Stoicism level and reminder time.

- [ ] **Step 1: Write `feature/onboarding/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.onboarding"
}

dependencies {
    implementation(project(":core:notifications"))
}
```

- [ ] **Step 2: Write `OnboardingUiState.kt`**

```kotlin
package com.stoicjournal.feature.onboarding

import com.stoicjournal.core.domain.model.StoicismLevel

data class OnboardingUiState(
    val step: Int = 0,
    val selectedLevel: StoicismLevel = StoicismLevel.BEGINNER,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val isLoading: Boolean = false
)
```

- [ ] **Step 3: Write `OnboardingViewModel.kt`**

```kotlin
package com.stoicjournal.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    fun selectLevel(level: StoicismLevel) {
        _uiState.update { it.copy(selectedLevel = level) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        _uiState.update { it.copy(reminderHour = hour, reminderMinute = minute) }
    }

    fun nextStep() {
        _uiState.update { it.copy(step = it.step + 1) }
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val state = _uiState.value
            preferencesRepository.setStoicismLevel(state.selectedLevel)
            preferencesRepository.setReminderTime(state.reminderHour, state.reminderMinute)
            preferencesRepository.setReminderEnabled(true)
            reminderScheduler.schedule(state.reminderHour, state.reminderMinute)
            preferencesRepository.setOnboardingComplete(true)
            onComplete()
        }
    }
}
```

- [ ] **Step 4: Write `OnboardingScreen.kt`**

A simple 3-step pager with:
1. Welcome + purpose
2. Stoicism level selection (chips)
3. Reminder time picker (TimePicker dialog or simple hour/minute spinners)

```kotlin
package com.stoicjournal.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
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
                label = { Text(level.name.lowercase().replaceFirstChar { it.uppercase() }) },
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
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
            onComplete()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Start journaling")
    }
}
```

- [ ] **Step 5: Build onboarding module**

Run: `./gradlew :feature:onboarding:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add feature/onboarding/
git commit -m "feat(onboarding): add 3-step onboarding with level and reminder"
```

---

