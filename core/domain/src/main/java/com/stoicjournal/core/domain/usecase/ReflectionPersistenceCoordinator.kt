package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.model.ReflectionCandidate
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.repository.StoicMentorRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReflectionPersistenceCoordinator @Inject constructor(
    private val journalRepository: JournalRepository,
    private val mentorRepository: StoicMentorRepository,
    private val validator: ReflectionCandidateValidator
) {
    suspend fun saveReflectionCandidate(
        candidate: ReflectionCandidate
    ): Result<JournalEntry> {
        val validation = validator.validate(candidate)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull()!!)
        }

        val entryId = UUID.randomUUID().toString()
        val journalBody = buildString {
            append(candidate.summary)
            if (candidate.keyInsights.isNotBlank()) {
                append("\n\nKey Insights:\n").append(candidate.keyInsights)
            }
            if (candidate.withinControl.isNotBlank()) {
                append("\n\nWithin My Control:\n").append(candidate.withinControl)
            }
            if (candidate.outsideControl.isNotBlank()) {
                append("\n\nOutside My Control:\n").append(candidate.outsideControl)
            }
            if (candidate.nextAction.isNotBlank()) {
                append("\n\nIntention:\n").append(candidate.nextAction)
            }
        }

        val journalEntry = JournalEntry(
            id = entryId,
            type = EntryType.GUIDED,
            promptId = null,
            title = candidate.title,
            body = journalBody,
            quoteId = null,
            exerciseId = null,
            virtues = candidate.virtues,
            createdAt = candidate.generatedAt,
            updatedAt = Instant.now()
        )

        return try {
            journalRepository.saveEntry(journalEntry)
            Result.success(journalEntry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
