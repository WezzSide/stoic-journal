package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.model.MentorReflectionLink
import com.stoicjournal.core.domain.model.ReflectionCandidate
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.repository.MentorReflectionLinkRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReflectionPersistenceCoordinator @Inject constructor(
    private val journalRepository: JournalRepository,
    private val linkRepository: MentorReflectionLinkRepository,
    private val validator: ReflectionCandidateValidator
) {
    suspend fun saveReflectionCandidate(
        candidate: ReflectionCandidate,
        savePolicy: String = "REVIEW_REQUIRED"
    ): Result<JournalEntry> {
        val validation = validator.validate(candidate)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull()!!)
        }

        val existingLink = linkRepository.getLink(candidate.conversationId, candidate.candidateId)
        if (existingLink != null) {
            val existingEntry = journalRepository.getEntryById(existingLink.journalEntryId)
            if (existingEntry != null) {
                return Result.success(existingEntry)
            }
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

            val link = MentorReflectionLink(
                id = UUID.randomUUID().toString(),
                conversationId = candidate.conversationId,
                candidateId = candidate.candidateId,
                journalEntryId = entryId,
                createdAt = Instant.now(),
                savePolicy = savePolicy
            )
            linkRepository.saveLink(link)

            Result.success(journalEntry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
