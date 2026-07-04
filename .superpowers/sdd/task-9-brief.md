## Task 9: Implement Today / Home feature

**Files:**
- Create: `feature/today/src/main/java/com/stoicjournal/feature/today/TodayScreen.kt`
- Create: `feature/today/src/main/java/com/stoicjournal/feature/today/TodayViewModel.kt`
- Create: `feature/today/src/main/java/com/stoicjournal/feature/today/TodayUiState.kt`
- Create: `feature/today/src/main/java/com/stoicjournal/feature/today/components/DailyPromptCard.kt`
- Create: `feature/today/src/main/java/com/stoicjournal/feature/today/components/QuickActionsRow.kt`
- Create: `feature/today/src/main/java/com/stoicjournal/feature/today/components/StreakIndicator.kt`
- Create: `feature/today/build.gradle.kts`

**Interfaces:**
- Consumes: `GetTodayPromptUseCase`, `GetQuoteForPromptUseCase`, `GetStreakUseCase`, `ExerciseRepository`.
- Produces: `TodayScreen` with callbacks for navigation.

- [ ] **Step 1: Write `feature/today/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.today"
}
```

- [ ] **Step 2: Write `TodayUiState.kt`**

```kotlin
package com.stoicjournal.feature.today

import com.stoicjournal.core.domain.model.Exercise
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.model.Streak

data class TodayUiState(
    val prompt: Prompt? = null,
    val quote: Quote? = null,
    val streak: Streak = Streak(0, null),
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true
)
```

- [ ] **Step 3: Write `TodayViewModel.kt`**

```kotlin
package com.stoicjournal.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.repository.ExerciseRepository
import com.stoicjournal.core.domain.usecase.GetQuoteForPromptUseCase
import com.stoicjournal.core.domain.usecase.GetStreakUseCase
import com.stoicjournal.core.domain.usecase.GetTodayPromptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    getTodayPromptUseCase: GetTodayPromptUseCase,
    private val getQuoteForPromptUseCase: GetQuoteForPromptUseCase,
    getStreakUseCase: GetStreakUseCase,
    exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState

    init {
        combine(
            getTodayPromptUseCase(),
            getStreakUseCase(),
            exerciseRepository.getAllExercises()
        ) { todayPrompt, streak, exercises ->
            Triple(todayPrompt, streak, exercises)
        }.onEach { (todayPrompt, streak, exercises) ->
            _uiState.update {
                it.copy(
                    prompt = todayPrompt.prompt,
                    streak = streak,
                    exercises = exercises,
                    isLoading = false
                )
            }
            getQuoteForPromptUseCase(todayPrompt.prompt).onEach { quote ->
                _uiState.update { it.copy(quote = quote) }
            }.launchIn(viewModelScope)
        }.launchIn(viewModelScope)
    }
}
```

- [ ] **Step 4: Write `DailyPromptCard.kt`**

```kotlin
package com.stoicjournal.feature.today.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote

@Composable
fun DailyPromptCard(
    prompt: Prompt,
    quote: Quote?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            quote?.let {
                Text(
                    text = "\"${it.text}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "— ${it.attribution}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Text(
                text = prompt.text,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
```

- [ ] **Step 5: Write `QuickActionsRow.kt`**

```kotlin
package com.stoicjournal.feature.today.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun QuickActionsRow(
    onFreeForm: () -> Unit,
    onExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onFreeForm, modifier = Modifier.weight(1f)) {
            Text("Free-form")
        }
        Button(onClick = onExercise, modifier = Modifier.weight(1f)) {
            Text("Exercise")
        }
    }
}
```

- [ ] **Step 6: Write `StreakIndicator.kt`**

```kotlin
package com.stoicjournal.feature.today.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import com.stoicjournal.core.domain.model.Streak

@Composable
fun StreakIndicator(streak: Streak) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${streak.currentCount}",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 32.sp
        )
        Text(
            text = " day streak",
            style = MaterialTheme.typography.bodyMedium,
            modifier = androidx.compose.ui.Modifier.padding(start = 4.dp)
        )
    }
}
```

- [ ] **Step 7: Write `TodayScreen.kt`**

```kotlin
package com.stoicjournal.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.feature.today.components.DailyPromptCard
import com.stoicjournal.feature.today.components.QuickActionsRow
import com.stoicjournal.feature.today.components.StreakIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onGuidedJournal: (String) -> Unit,
    onFreeFormJournal: () -> Unit,
    onExercise: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stoic Journal") },
                actions = {
                    IconButton(onClick = onNavigateToLibrary) {
                        Icon(Icons.Default.LibraryBooks, contentDescription = "Library")
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
                    val exerciseId = uiState.exercises.firstOrNull()?.id ?: return@QuickActionsRow
                    onExercise(exerciseId)
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.exercises.isNotEmpty()) {
                Text(
                    text = "Stoic Exercises",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                uiState.exercises.forEach { exercise ->
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(exercise.title) },
                        supportingContent = { Text(exercise.description) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            IconButton(onClick = { onExercise(exercise.id) }) {
                                Icon(Icons.Default.LibraryBooks, contentDescription = "Start exercise")
                            }
                        }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 8: Build today module**

Run: `./gradlew :feature:today:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add feature/today/
git commit -m "feat(today): add home screen with daily prompt, streak, and quick actions"
```

---

