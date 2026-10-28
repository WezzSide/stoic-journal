package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.model.ReflectionCandidate

interface ReflectionPersistenceCoordinator {
    suspend fun saveReflectionCandidate(
        candidate: ReflectionCandidate,
        savePolicy: String = "REVIEW_REQUIRED"
    ): Result<JournalEntry>
}
