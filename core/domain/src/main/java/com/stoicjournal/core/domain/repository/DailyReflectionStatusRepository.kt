package com.stoicjournal.core.domain.repository

import java.time.LocalDate

interface DailyReflectionStatusRepository {
    suspend fun hasCompletedReflection(date: LocalDate): Boolean
}
