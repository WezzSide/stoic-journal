package com.stoicjournal.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Today : Screen("today")
    data object GuidedJournal : Screen("guided_journal/{promptId}") {
        fun createRoute(promptId: String) = "guided_journal/$promptId"
    }
    data object FreeFormJournal : Screen("free_form_journal")
    data object ExerciseJournal : Screen("exercise_journal/{exerciseId}") {
        fun createRoute(exerciseId: String) = "exercise_journal/$exerciseId"
    }
    data object Library : Screen("library")
    data object QuoteDetail : Screen("quote_detail/{quoteId}") {
        fun createRoute(quoteId: String) = "quote_detail/$quoteId"
    }
    data object History : Screen("history")
    data object EntryDetail : Screen("entry_detail/{entryId}") {
        fun createRoute(entryId: String) = "entry_detail/$entryId"
    }
    data object Settings : Screen("settings")
    data object AiReflection : Screen("ai_reflection")
}
