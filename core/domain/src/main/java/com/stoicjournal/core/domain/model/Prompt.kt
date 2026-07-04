package com.stoicjournal.core.domain.model

data class Prompt(
    val id: String,
    val text: String,
    val theme: Theme,
    val philosopherId: String? = null,
    val difficultyLevel: StoicismLevel = StoicismLevel.BEGINNER
)
