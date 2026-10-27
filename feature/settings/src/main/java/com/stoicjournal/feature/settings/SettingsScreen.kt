package com.stoicjournal.feature.settings

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

            // --- Sectie 3: Data Backup & Export ---
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
