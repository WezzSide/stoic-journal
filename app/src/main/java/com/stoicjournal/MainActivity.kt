package com.stoicjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoicjournal.navigation.Screen
import com.stoicjournal.navigation.StoicJournalNavHost
import com.stoicjournal.theme.StoicJournalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StoicJournalTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val startDestination by viewModel.startDestination.collectAsState()
                startDestination?.let { destination ->
                    StoicJournalNavHost(startDestination = destination)
                }
            }
        }
    }
}
