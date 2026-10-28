package com.stoicjournal.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stoicjournal.feature.history.HistoryScreen
import com.stoicjournal.feature.history.EntryDetailScreen
import com.stoicjournal.feature.ai.AiReflectionScreen
import com.stoicjournal.feature.ai.mentor.MentorChatScreen
import com.stoicjournal.feature.ai.mentor.MentorConversationListScreen
import com.stoicjournal.feature.ai.mentor.MentorReflectionPreviewScreen
import com.stoicjournal.feature.journal.exercise.ExerciseJournalScreen
import com.stoicjournal.feature.journal.freeform.FreeFormJournalScreen
import com.stoicjournal.feature.journal.guided.GuidedJournalScreen
import com.stoicjournal.feature.library.LibraryScreen
import com.stoicjournal.feature.library.QuoteDetailScreen
import com.stoicjournal.feature.onboarding.OnboardingScreen
import com.stoicjournal.feature.settings.SettingsScreen
import com.stoicjournal.feature.settings.memory.MemorySettingsScreen
import com.stoicjournal.core.common.notifications.NotificationRoutes
import androidx.compose.runtime.LaunchedEffect
import com.stoicjournal.feature.today.TodayScreen

@Composable
fun StoicJournalNavHost(
    startDestination: Screen,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    pendingRoute: String? = null,
    onPendingRouteConsumed: () -> Unit = {}
) {
    // Notification deep links: rendered only here, i.e. behind the app-lock gate in MainActivity.
    LaunchedEffect(pendingRoute) {
        when (pendingRoute) {
            NotificationRoutes.ROUTE_MENTOR_CHAT -> {
                navController.navigate(Screen.MentorConversationList.route) { launchSingleTop = true }
                navController.navigate(Screen.MentorChat.createRoute(java.util.UUID.randomUUID().toString()))
                onPendingRouteConsumed()
            }
            null -> Unit
            else -> {
                Log.w(TAG, "Ignoring unknown notification route")
                onPendingRouteConsumed()
            }
        }
    }
    NavHost(
        navController = navController,
        startDestination = startDestination.route,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Today.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Today.route) {
            TodayScreen(
                onGuidedJournal = { promptId ->
                    navController.navigate(Screen.GuidedJournal.createRoute(promptId))
                },
                onFreeFormJournal = { navController.navigate(Screen.FreeFormJournal.route) },
                onExercise = { exerciseId ->
                    navController.navigate(Screen.ExerciseJournal.createRoute(exerciseId))
                },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAiReflection = { navController.navigate(Screen.MentorConversationList.route) }
            )
        }
        composable(Screen.GuidedJournal.route) { backStackEntry ->
            val promptId = backStackEntry.arguments?.getString("promptId")
            if (promptId == null) {
                Log.e(TAG, "Missing promptId argument for GuidedJournal")
                navController.popBackStack()
                return@composable
            }
            GuidedJournalScreen(
                promptId = promptId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.FreeFormJournal.route) {
            FreeFormJournalScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ExerciseJournal.route) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId")
            if (exerciseId == null) {
                Log.e(TAG, "Missing exerciseId argument for ExerciseJournal")
                navController.popBackStack()
                return@composable
            }
            ExerciseJournalScreen(
                exerciseId = exerciseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onQuoteClick = { quoteId ->
                    navController.navigate(Screen.QuoteDetail.createRoute(quoteId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.QuoteDetail.route) { backStackEntry ->
            val quoteId = backStackEntry.arguments?.getString("quoteId")
            if (quoteId == null) {
                Log.e(TAG, "Missing quoteId argument for QuoteDetail")
                navController.popBackStack()
                return@composable
            }
            QuoteDetailScreen(
                quoteId = quoteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onEntryClick = { entryId ->
                    navController.navigate(Screen.EntryDetail.createRoute(entryId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.EntryDetail.route) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId")
            if (entryId == null) {
                Log.e(TAG, "Missing entryId argument for EntryDetail")
                navController.popBackStack()
                return@composable
            }
            EntryDetailScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMemorySettings = { navController.navigate(Screen.MemorySettings.route) }
            )
        }
        composable(Screen.MemorySettings.route) {
            MemorySettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AiReflection.route) {
            AiReflectionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.MentorChat.route) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: java.util.UUID.randomUUID().toString()
            MentorChatScreen(
                conversationId = conversationId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreview = { candidateId -> 
                    navController.navigate(Screen.MentorReflectionPreview.createRoute(candidateId)) 
                }
            )
        }
        composable(Screen.MentorConversationList.route) {
            MentorConversationListScreen(
                onNavigateBack = { navController.popBackStack() },
                onSelectConversation = { id -> navController.navigate(Screen.MentorChat.createRoute(id)) },
                onNewConversation = { navController.navigate(Screen.MentorChat.createRoute(java.util.UUID.randomUUID().toString())) }
            )
        }
        composable(Screen.MentorReflectionPreview.route) { backStackEntry ->
            val candidateId = backStackEntry.arguments?.getString("candidateId") ?: ""
            MentorReflectionPreviewScreen(
                candidateId = candidateId,
                onNavigateBack = { navController.popBackStack() },
                onContinueConversation = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.navigate(Screen.Today.route) {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}

private const val TAG = "StoicJournalNavHost"
