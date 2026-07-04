package com.stoicjournal.core.domain.model

data class Exercise(
    val id: String,
    val title: String,
    val description: String,
    val steps: List<String>,
    val theme: Theme,
    val difficultyLevel: StoicismLevel = StoicismLevel.BEGINNER
)
