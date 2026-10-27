package com.stoicjournal

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.stoicjournal.navigation.StoicJournalNavHost
import com.stoicjournal.security.BiometricPromptManager
import com.stoicjournal.security.LockScreen
import com.stoicjournal.theme.StoicJournalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private lateinit var biometricPromptManager: BiometricPromptManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        biometricPromptManager = BiometricPromptManager(this)

        setContent {
            StoicJournalTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val startDestination by viewModel.startDestination.collectAsState()
                val userPrefs by viewModel.userPreferences.collectAsState()
                val isLocked by viewModel.isLocked.collectAsState()

                // Register ProcessLifecycleObserver for lock timeouts
                LaunchedEffect(Unit) {
                    ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onStop(owner: LifecycleOwner) {
                            viewModel.onAppBackgrounded()
                        }

                        override fun onStart(owner: LifecycleOwner) {
                            viewModel.onAppForegrounded()
                        }
                    })
                }

                // Privacy Screen (FLAG_SECURE)
                LaunchedEffect(userPrefs?.privacyScreenEnabled) {
                    if (userPrefs?.privacyScreenEnabled == true) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                // Biometric auto-trigger on lock
                LaunchedEffect(isLocked, userPrefs?.biometricLockEnabled) {
                    if (isLocked && userPrefs?.biometricLockEnabled == true && biometricPromptManager.canAuthenticate()) {
                        biometricPromptManager.showBiometricPrompt(
                            onSuccess = { viewModel.unlock() },
                            onError = { /* fallback to PIN / manual trigger */ }
                        )
                    }
                }

                if (isLocked) {
                    LockScreen(
                        showBiometricButton = (userPrefs?.biometricLockEnabled == true) && biometricPromptManager.canAuthenticate(),
                        onPinSubmitted = { pin ->
                            viewModel.unlockWithPin(pin)
                        },
                        onBiometricRequested = {
                            biometricPromptManager.showBiometricPrompt(
                                onSuccess = { viewModel.unlock() },
                                onError = { }
                            )
                        }
                    )
                } else {
                    val destination = startDestination
                    if (destination != null) {
                        StoicJournalNavHost(startDestination = destination)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
