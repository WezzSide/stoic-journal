package com.stoicjournal.core.domain.model

data class Quote(
    val id: String,
    val text: String,
    val attribution: String,
    val philosopherId: String,
    val theme: Theme,
    val source: String? = null,
    val isFavorite: Boolean = false
)
