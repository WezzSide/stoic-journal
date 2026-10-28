package com.stoicjournal.feature.settings

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.feature.settings.components.PinSetupDialog
import com.stoicjournal.feature.settings.components.ReminderTimePicker
import kotlinx.coroutines.launch
import java.util.Locale

private enum class ReminderPickerType { MORNING, EVENING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMemorySettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var activePickerType by remember { mutableStateOf<ReminderPickerType?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pendingEnable by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val errorMessage = uiState.error
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.errorShown()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (pendingEnable) {
            pendingEnable = false
            if (isGranted) {
                viewModel.setMorningReminderEnabled(true)
                viewModel.setEveningReminderEnabled(true)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Notificatietoestemming is vereist voor herinneringen.")
                }
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportBackup(it, context) {
                scope.launch {
                    snackbarHostState.showSnackbar("Backup succesvol geëxporteerd! 💾")
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importBackup(it, context) {
                scope.launch {
                    snackbarHostState.showSnackbar("Backup succesvol geïmporteerd! 🏛️")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instellingen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Sectie 1: Meldingen & Herinneringen ---
            Text(
                text = "Herinneringen & Reflecties",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            // Ochtend-intentie
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ochtend-intentie (08:00)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d - Premeditatio Malorum", uiState.morningReminderHour, uiState.morningReminderMinute),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.morningReminderEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            viewModel.setMorningReminderEnabled(false)
                            return@Switch
                        }

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            viewModel.setMorningReminderEnabled(true)
                            return@Switch
                        }

                        val permission = Manifest.permission.POST_NOTIFICATIONS
                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.setMorningReminderEnabled(true)
                        } else {
                            pendingEnable = true
                            notificationPermissionLauncher.launch(permission)
                            viewModel.setNotificationPermissionRequested(true)
                        }
                    }
                )
            }
            if (uiState.morningReminderEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                        .clickable { activePickerType = ReminderPickerType.MORNING },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Ochtendtijd aanpassen",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Avond-reflectie
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Avond-reflectie (21:00)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d - Seneca's Avondreview", uiState.eveningReminderHour, uiState.eveningReminderMinute),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.eveningReminderEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            viewModel.setEveningReminderEnabled(false)
                            return@Switch
                        }

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            viewModel.setEveningReminderEnabled(true)
                            return@Switch
                        }

                        val permission = Manifest.permission.POST_NOTIFICATIONS
                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.setEveningReminderEnabled(true)
                        } else {
                            pendingEnable = true
                            notificationPermissionLauncher.launch(permission)
                            viewModel.setNotificationPermissionRequested(true)
                        }
                    }
                )
            }
            if (uiState.eveningReminderEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                        .clickable { activePickerType = ReminderPickerType.EVENING },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Avondtijd aanpassen",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // --- Sectie 2: Beveiliging & Privacy ---
            Text(
                text = "Beveiliging & Privacy",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
            )

            // Biometrische Lock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Biometrische vergrendeling", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Vergrendel dagboek met vingerafdruk of gezichtsherkenning",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.biometricLockEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.setBiometricLockEnabled(enabled)
                    }
                )
            }

            // PIN Lock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PIN-code vergrendeling", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (uiState.hasPinSet) "4-cijferige PIN ingesteld" else "Stel een 4-cijferige PIN in",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.pinLockEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !uiState.hasPinSet) {
                            showPinDialog = true
                        } else {
                            viewModel.setPinLockEnabled(enabled)
                        }
                    }
                )
            }

            if (uiState.pinLockEnabled || uiState.hasPinSet) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable { showPinDialog = true },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (uiState.hasPinSet) "PIN-code wijzigen" else "PIN-code instellen",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Vergrendel Timeout
            if (uiState.biometricLockEnabled || uiState.pinLockEnabled) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Automatisch vergrendelen na op achtergrond", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = uiState.lockTimeoutSeconds == 0,
                            onClick = { viewModel.setLockTimeoutSeconds(0) },
                            label = { Text("Direct") }
                        )
                        FilterChip(
                            selected = uiState.lockTimeoutSeconds == 60,
                            onClick = { viewModel.setLockTimeoutSeconds(60) },
                            label = { Text("1 minuut") }
                        )
                        FilterChip(
                            selected = uiState.lockTimeoutSeconds == 300,
                            onClick = { viewModel.setLockTimeoutSeconds(300) },
                            label = { Text("5 minuten") }
                        )
                    }
                }
            }

            // Privacy Screen (FLAG_SECURE)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Privacy scherm (FLAG_SECURE)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Blokkeer screenshots en verberg app-inhoud in recente apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.privacyScreenEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.setPrivacyScreenEnabled(enabled)
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // --- Sectie 3: AI & Stoic Mentor Providers ---
            Text(
                text = "AI & Stoic Mentor Providers",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
            )

            var selectedProvider by remember { mutableStateOf("openai") }
            var apiKeyInput by remember { mutableStateOf("") }
            var isTestingConnection by remember { mutableStateOf(false) }
            var pendingActiveProvider by remember { mutableStateOf<String?>(null) }
            var showProactiveTimePicker by remember { mutableStateOf(false) }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("API-sleutel per provider", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsViewModel.PROVIDER_IDS.forEach { id ->
                        FilterChip(
                            selected = selectedProvider == id,
                            onClick = { selectedProvider = id },
                            label = { Text(providerLabel(id) + if (id in uiState.configuredProviderIds) " ✓" else "") },
                            modifier = Modifier.testTag("KeyChip_$id")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("${providerLabel(selectedProvider)} API Key") },
                    placeholder = { Text("sk-...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ApiKeyField"),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedProvider in uiState.configuredProviderIds) {
                        TextButton(
                            onClick = {
                                viewModel.deleteAiApiKey(selectedProvider) { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            },
                            modifier = Modifier.testTag("DeleteKeyButton")
                        ) { Text("Verwijder sleutel") }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Button(
                        onClick = {
                            if (apiKeyInput.isNotBlank()) {
                                isTestingConnection = true
                                viewModel.saveAiApiKey(selectedProvider, apiKeyInput) { _, msg ->
                                    isTestingConnection = false
                                    apiKeyInput = ""
                                    scope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            }
                        },
                        enabled = apiKeyInput.isNotBlank() && !isTestingConnection,
                        modifier = Modifier.testTag("SaveKeyButton")
                    ) {
                        Text(if (isTestingConnection) "Verbinden..." else "Sleutel Opslaan & Testen")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Actieve provider voor Stoic Mentor", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (uiState.activeProviderExplicitlyChosen) {
                        "Gesprekken worden verstuurd naar ${providerLabel(uiState.activeProviderId)}."
                    } else {
                        "Nog geen keuze gemaakt; standaard ${providerLabel(uiState.activeProviderId)}. Kies hieronder expliciet."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("ActiveProviderStatus")
                )
                SettingsViewModel.PROVIDER_IDS.forEach { id ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { if (id != uiState.activeProviderId) pendingActiveProvider = id }
                            .testTag("ActiveProviderRow_$id"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.activeProviderId == id,
                            onClick = { if (id != uiState.activeProviderId) pendingActiveProvider = id }
                        )
                        Text(providerLabel(id), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            if (id in uiState.configuredProviderIds) "sleutel aanwezig" else "geen sleutel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            pendingActiveProvider?.let { candidate ->
                AlertDialog(
                    onDismissRequest = { pendingActiveProvider = null },
                    title = { Text("Provider wisselen?") },
                    text = {
                        Text(
                            "Vanaf nu worden je Stoic Mentor-gesprekken (en alleen wat je in het gesprek typt, plus de " +
                                "context die je in Geheugen & Context hebt toegestaan) naar ${providerLabel(candidate)} gestuurd."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.setActiveAiProvider(candidate)
                                pendingActiveProvider = null
                            },
                            modifier = Modifier.testTag("ConfirmProviderSwitch")
                        ) { Text("Bevestigen") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingActiveProvider = null }) { Text("Annuleren") }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // --- Sectie 3b: Proactieve Stoic Mentor ---
            Text(
                text = "Proactieve Stoic Mentor",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dagelijkse uitnodiging tot reflectie", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Een lokale melding op je gekozen tijd. Geen AI-verzoeken op de achtergrond, geen dagboektekst in de melding.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.proactiveMentorEnabled,
                    onCheckedChange = { viewModel.setProactiveMentorEnabled(it) },
                    modifier = Modifier.testTag("ProactiveMentorSwitch")
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(enabled = uiState.proactiveMentorEnabled) { showProactiveTimePicker = true }
                    .padding(horizontal = 16.dp)
                    .testTag("ProactiveTimeRow"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Herinneringstijd", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    String.format(java.util.Locale.ROOT, "%02d:%02d", uiState.proactiveReminderHour, uiState.proactiveReminderMinute),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stille uren", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                HourStepper(
                    label = "van",
                    hour = uiState.proactiveQuietStartHour,
                    enabled = uiState.proactiveMentorEnabled,
                    tag = "QuietStart",
                    onChange = { viewModel.setProactiveQuietHours(it, uiState.proactiveQuietEndHour) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                HourStepper(
                    label = "tot",
                    hour = uiState.proactiveQuietEndHour,
                    enabled = uiState.proactiveMentorEnabled,
                    tag = "QuietEnd",
                    onChange = { viewModel.setProactiveQuietHours(uiState.proactiveQuietStartHour, it) }
                )
            }
            if (showProactiveTimePicker) {
                ReminderTimePicker(
                    initialHour = uiState.proactiveReminderHour,
                    initialMinute = uiState.proactiveReminderMinute,
                    onConfirm = { h, m ->
                        viewModel.setProactiveReminderTime(h, m)
                        showProactiveTimePicker = false
                    },
                    onDismiss = { showProactiveTimePicker = false }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onNavigateToMemorySettings() }
                    .padding(horizontal = 16.dp)
                    .testTag("MemorySettingsRow"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Geheugen & context", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Beheer wat Stoic Mentor mag onthouden en of historische context naar de provider mag.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // --- Sectie 4: Data Backup & Export ---
            Text(
                text = "Data Backup & Export",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { exportBackupLauncher.launch("stoic_journal_backup.json") },
                contentAlignment = Alignment.CenterStart
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Exporteer dagboek (JSON)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Sla al je dagboeknotities lokaal op als een JSON-bestand", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                contentAlignment = Alignment.CenterStart
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Importeer dagboek (JSON)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Herstel dagboeknotities vanuit een eerder geëxporteerd bestand", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // --- Sectie 4: Systeem & Onboarding ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { viewModel.resetOnboarding() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Onboarding opnieuw uitvoeren",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = "Stoic Journal v1.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    activePickerType?.let { type ->
        val initialHour = if (type == ReminderPickerType.MORNING) uiState.morningReminderHour else uiState.eveningReminderHour
        val initialMinute = if (type == ReminderPickerType.MORNING) uiState.morningReminderMinute else uiState.eveningReminderMinute

        ReminderTimePicker(
            initialHour = initialHour,
            initialMinute = initialMinute,
            onConfirm = { hour, minute ->
                if (type == ReminderPickerType.MORNING) {
                    viewModel.setMorningReminderTime(hour, minute)
                } else {
                    viewModel.setEveningReminderTime(hour, minute)
                }
                activePickerType = null
            },
            onDismiss = { activePickerType = null }
        )
    }

    if (showPinDialog) {
        PinSetupDialog(
            onConfirm = { pin ->
                viewModel.setPinCode(pin)
                showPinDialog = false
            },
            onDismiss = { showPinDialog = false }
        )
    }
}

private fun providerLabel(id: String): String = when (id) {
    "openai" -> "OpenAI"
    "claude" -> "Claude"
    "gemini" -> "Gemini"
    else -> id
}

@Composable
private fun HourStepper(
    label: String,
    hour: Int,
    enabled: Boolean,
    tag: String,
    onChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(
            onClick = { onChange((hour + 23) % 24) },
            enabled = enabled,
            modifier = Modifier.testTag("${tag}Minus")
        ) { Text("−") }
        Text(
            String.format(java.util.Locale.ROOT, "%02d", hour),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("${tag}Value")
        )
        IconButton(
            onClick = { onChange((hour + 1) % 24) },
            enabled = enabled,
            modifier = Modifier.testTag("${tag}Plus")
        ) { Text("+") }
    }
}
