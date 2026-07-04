## Task 10: Implement Journal feature

**Files:**
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/components/JournalTextField.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/components/QuoteHeader.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/components/SaveDiscardBar.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/guided/GuidedJournalScreen.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/guided/GuidedJournalViewModel.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/freeform/FreeFormJournalScreen.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/freeform/FreeFormJournalViewModel.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/exercise/ExerciseJournalScreen.kt`
- Create: `feature/journal/src/main/java/com/stoicjournal/feature/journal/exercise/ExerciseJournalViewModel.kt`
- Create: `feature/journal/build.gradle.kts`

**Interfaces:**
- Consumes: `PromptRepository`, `ExerciseRepository`, `QuoteRepository`, `SaveJournalEntryUseCase`, `GetQuoteForPromptUseCase`.
- Produces: Three journal entry screens that save `JournalEntry` to the repository and navigate back.

- [ ] **Step 1: Write `feature/journal/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.feature")
}

android {
    namespace = "com.stoicjournal.feature.journal"
}
```

- [ ] **Step 2: Write shared journal components**

`JournalTextField.kt`:

```kotlin
package com.stoicjournal.feature.journal.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

@Composable
fun JournalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            if (value.isEmpty() && placeholder != null) {
                placeholder()
            }
            innerTextField()
        }
    )
}
```

`QuoteHeader.kt`:

```kotlin
package com.stoicjournal.feature.journal.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoicjournal.core.domain.model.Quote

@Composable
fun QuoteHeader(quote: Quote?, modifier: Modifier = Modifier) {
    if (quote == null) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "\"${quote.text}\"",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "— ${quote.attribution}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
```

`SaveDiscardBar.kt`:

```kotlin
package com.stoicjournal.feature.journal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SaveDiscardBar(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    saveEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) {
            Text("Discard")
        }
        Button(
            onClick = onSave,
            enabled = saveEnabled,
            modifier = Modifier.weight(1f)
        ) {
            Text("Save")
        }
    }
}
```

- [ ] **Step 3: Implement guided journal**

`GuidedJournalViewModel.kt`:

```kotlin
package com.stoicjournal.feature.journal.guided

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.Prompt
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.PromptRepository
import com.stoicjournal.core.domain.usecase.GetQuoteForPromptUseCase
import com.stoicjournal.core.domain.usecase.SaveJournalEntryUseCase
import com.stoicjournal.core.domain.usecase.createJournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuidedJournalUiState(
    val prompt: Prompt? = null,
    val quote: Quote? = null,
    val body: String = "",
    val isSaving: Boolean = false
)

@HiltViewModel
class GuidedJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    promptRepository: PromptRepository,
    private val getQuoteForPromptUseCase: GetQuoteForPromptUseCase,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase
) : ViewModel() {

    private val promptId: String = savedStateHandle["promptId"] ?: ""
    private val _uiState = MutableStateFlow(GuidedJournalUiState())
    val uiState: StateFlow<GuidedJournalUiState> = _uiState

    init {
        viewModelScope.launch {
            val prompt = promptRepository.getAllPrompts().first().find { it.id == promptId }
            _uiState.update { it.copy(prompt = prompt) }
            prompt?.let { p ->
                getQuoteForPromptUseCase(p).onEach { quote ->
                    _uiState.update { it.copy(quote = quote) }
                }.launchIn(viewModelScope)
            }
        }
    }

    fun updateBody(body: String) {
        _uiState.update { it.copy(body = body) }
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val entry = createJournalEntry(
                type = EntryType.GUIDED,
                body = state.body,
                promptId = state.prompt?.id,
                quoteId = state.quote?.id
            )
            saveJournalEntryUseCase(entry)
            onComplete()
        }
    }
}
```

`GuidedJournalScreen.kt`:

```kotlin
package com.stoicjournal.feature.journal.guided

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.stoicjournal.feature.journal.components.JournalTextField
import com.stoicjournal.feature.journal.components.QuoteHeader
import com.stoicjournal.feature.journal.components.SaveDiscardBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedJournalScreen(
    promptId: String,
    onNavigateBack: () -> Unit,
    viewModel: GuidedJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Guided Reflection") })
        },
        bottomBar = {
            SaveDiscardBar(
                onSave = { viewModel.save(onNavigateBack) },
                onDiscard = onNavigateBack,
                saveEnabled = uiState.body.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            QuoteHeader(quote = uiState.quote)
            uiState.prompt?.let { prompt ->
                Text(
                    text = prompt.text,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            JournalTextField(
                value = uiState.body,
                onValueChange = viewModel::updateBody,
                placeholder = {
                    Text(
                        "Write your reflection...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

- [ ] **Step 4: Implement free-form journal**

`FreeFormJournalViewModel.kt`:

```kotlin
package com.stoicjournal.feature.journal.freeform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.Quote
import com.stoicjournal.core.domain.repository.QuoteRepository
import com.stoicjournal.core.domain.usecase.SaveJournalEntryUseCase
import com.stoicjournal.core.domain.usecase.createJournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FreeFormJournalUiState(
    val title: String = "",
    val body: String = "",
    val attachedQuote: Quote? = null,
    val quotes: List<Quote> = emptyList()
)

@HiltViewModel
class FreeFormJournalViewModel @Inject constructor(
    quoteRepository: QuoteRepository,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FreeFormJournalUiState())
    val uiState: StateFlow<FreeFormJournalUiState> = _uiState

    init {
        quoteRepository.getAllQuotes().onEach { quotes ->
            _uiState.update { it.copy(quotes = quotes) }
        }.launchIn(viewModelScope)
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateBody(body: String) {
        _uiState.update { it.copy(body = body) }
    }

    fun attachRandomQuote() {
        val quotes = _uiState.value.quotes
        if (quotes.isNotEmpty()) {
            _uiState.update { it.copy(attachedQuote = quotes.random()) }
        }
    }

    fun removeQuote() {
        _uiState.update { it.copy(attachedQuote = null) }
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val entry = createJournalEntry(
                type = EntryType.FREE_FORM,
                body = state.body,
                title = state.title.takeIf { it.isNotBlank() },
                quoteId = state.attachedQuote?.id
            )
            saveJournalEntryUseCase(entry)
            onComplete()
        }
    }
}
```

`FreeFormJournalScreen.kt`:

```kotlin
package com.stoicjournal.feature.journal.freeform

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.feature.journal.components.JournalTextField
import com.stoicjournal.feature.journal.components.QuoteHeader
import com.stoicjournal.feature.journal.components.SaveDiscardBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeFormJournalScreen(
    onNavigateBack: () -> Unit,
    viewModel: FreeFormJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Free-form Entry") }) },
        bottomBar = {
            SaveDiscardBar(
                onSave = { viewModel.save(onNavigateBack) },
                onDiscard = onNavigateBack,
                saveEnabled = uiState.body.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Title (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            QuoteHeader(quote = uiState.attachedQuote)
            if (uiState.attachedQuote == null) {
                TextButton(onClick = viewModel::attachRandomQuote) {
                    Text("Add a Stoic quote")
                }
            } else {
                TextButton(onClick = viewModel::removeQuote) {
                    Text("Remove quote")
                }
            }
            JournalTextField(
                value = uiState.body,
                onValueChange = viewModel::updateBody,
                placeholder = {
                    Text(
                        "Write freely...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

- [ ] **Step 5: Implement exercise journal**

`ExerciseJournalViewModel.kt`:

```kotlin
package com.stoicjournal.feature.journal.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.Exercise
import com.stoicjournal.core.domain.repository.ExerciseRepository
import com.stoicjournal.core.domain.usecase.SaveJournalEntryUseCase
import com.stoicjournal.core.domain.usecase.createJournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseJournalUiState(
    val exercise: Exercise? = null,
    val reflection: String = ""
)

@HiltViewModel
class ExerciseJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase
) : ViewModel() {

    private val exerciseId: String = savedStateHandle["exerciseId"] ?: ""
    private val _uiState = MutableStateFlow(ExerciseJournalUiState())
    val uiState: StateFlow<ExerciseJournalUiState> = _uiState

    init {
        viewModelScope.launch {
            val exercise = exerciseRepository.getAllExercises().first().find { it.id == exerciseId }
            _uiState.update { it.copy(exercise = exercise) }
        }
    }

    fun updateReflection(reflection: String) {
        _uiState.update { it.copy(reflection = reflection) }
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val entry = createJournalEntry(
                type = EntryType.EXERCISE,
                body = state.reflection,
                exerciseId = state.exercise?.id
            )
            saveJournalEntryUseCase(entry)
            onComplete()
        }
    }
}
```

`ExerciseJournalScreen.kt`:

```kotlin
package com.stoicjournal.feature.journal.exercise

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.stoicjournal.feature.journal.components.JournalTextField
import com.stoicjournal.feature.journal.components.SaveDiscardBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseJournalScreen(
    exerciseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExerciseJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(uiState.exercise?.title ?: "Exercise") }) },
        bottomBar = {
            SaveDiscardBar(
                onSave = { viewModel.save(onNavigateBack) },
                onDiscard = onNavigateBack,
                saveEnabled = uiState.reflection.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            uiState.exercise?.let { exercise ->
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                exercise.steps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            Text(
                text = "Reflection",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            JournalTextField(
                value = uiState.reflection,
                onValueChange = viewModel::updateReflection,
                placeholder = {
                    Text(
                        "Write your reflection...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

- [ ] **Step 6: Build journal module**

Run: `./gradlew :feature:journal:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add feature/journal/
git commit -m "feat(journal): add guided, free-form, and exercise journal entry screens"
```

---

