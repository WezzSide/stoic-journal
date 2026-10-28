package com.stoicjournal.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(
    showBiometricButton: Boolean,
    onPinSubmitted: (String) -> Boolean,
    onBiometricRequested: () -> Unit,
    modifier: Modifier = Modifier,
    lockoutUntilMillis: Long = 0L
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Tick once a second while PIN entry is throttled so the countdown and keypad re-enable live.
    LaunchedEffect(lockoutUntilMillis) {
        nowMillis = System.currentTimeMillis()
        while (nowMillis < lockoutUntilMillis) {
            delay(1_000)
            nowMillis = System.currentTimeMillis()
        }
    }
    val lockoutSeconds = ((lockoutUntilMillis - nowMillis).coerceAtLeast(0L) + 999) / 1000
    val isThrottled = lockoutSeconds > 0

    fun onNumberClicked(num: String) {
        if (isThrottled) return
        if (enteredPin.length < 4) {
            enteredPin += num
            errorMessage = null
            if (enteredPin.length == 4) {
                val success = onPinSubmitted(enteredPin)
                if (!success) {
                    errorMessage = "Onjuiste PIN-code, probeer opnieuw."
                    enteredPin = ""
                }
            }
        }
    }

    fun onBackPressed() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Vergrendeld",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Stoic Journal",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Bewaak je innerlijke rust",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // PIN dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            (if (isThrottled) "Te veel onjuiste pogingen. Probeer over $lockoutSeconds s opnieuw." else errorMessage)?.let { msg ->
                Text(
                    text = msg,
                    modifier = Modifier.testTag("lock_error"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } ?: Spacer(modifier = Modifier.height(18.dp))

            Spacer(modifier = Modifier.height(32.dp))

            // Numpad 1-9
            val numbers = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            )

            for (row in numbers) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    for (num in row) {
                        PinKeyButton(text = num, onClick = { onNumberClicked(num) })
                    }
                }
            }

            // Bottom row: Biometric, "0", Backspace
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBiometricButton) {
                    IconButton(
                        onClick = onBiometricRequested,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometrische ontgrendeling",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(64.dp))
                }

                PinKeyButton(text = "0", onClick = { onNumberClicked("0") })

                IconButton(
                    onClick = { onBackPressed() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Wissen",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
