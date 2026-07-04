package com.stoicjournal.core.domain.model

import java.time.LocalDate

data class Streak(
    val currentCount: Int,
    val lastEntryDate: LocalDate?
)
