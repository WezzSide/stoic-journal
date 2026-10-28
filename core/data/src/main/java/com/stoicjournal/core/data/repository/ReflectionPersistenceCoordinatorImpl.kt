package com.stoicjournal.core.data.repository

import androidx.room.withTransaction
import com.stoicjournal.core.data.dao.JournalEntryDao
import com.stoicjournal.core.data.dao.MentorReflectionLinkDao
import com.stoicjournal.core.data.database.StoicJournalDatabase
import com.stoicjournal.core.data.entity.JournalEntryEntity
import com.stoicjournal.core.data.entity.MentorReflectionLinkEntity
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.model.ReflectionCandidate
import com.stoicjournal.core.domain.repository.ReflectionPersistenceCoordinator
import com.stoicjournal.core.domain.usecase.ReflectionCandidateValidator
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReflectionPersistenceCoordinatorImpl @Inject constructor(
    private val db: StoicJournalDatabase,
    private val journalDao: JournalEntryDao,
    private val linkDao: MentorReflectionLinkDao,
    private val validator: ReflectionCandidateValidator
) : ReflectionPersistenceCoordinator {
    override suspend fun saveReflectionCandidate(
        candidate: ReflectionCandidate,
        savePolicy: String
    ): Result<JournalEntry> {
        val validation = validator.validate(candidate)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull()!!)
        }

        return try {
            db.withTransaction {
                val existingLink = linkDao.getLink(candidate.conversationId, candidate.candidateId)
                if (existingLink != null) {
                    val entity = journalDao.getById(existingLink.journalEntryId)
                    if (entity != null) {
                        return@withTransaction Result.success(entity.toDomainModel())
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

                val journalEntity = JournalEntryEntity(
                    id = entryId,
                    type = EntryType.GUIDED,
                    promptId = null,
                    title = candidate.title,
                    body = journalBody,
                    quoteId = null,
                    exerciseId = null,
                    virtues = candidate.virtues.joinToString(",") { it.name },
                    createdAtMillis = candidate.generatedAt.toEpochMilli(),
                    updatedAtMillis = Instant.now().toEpochMilli()
                )

                journalDao.insert(journalEntity)

                val linkEntity = MentorReflectionLinkEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = candidate.conversationId,
                    candidateId = candidate.candidateId,
                    journalEntryId = entryId,
                    createdAt = Instant.now().toEpochMilli(),
                    savePolicy = savePolicy
                )
                linkDao.insertLink(linkEntity)

                Result.success(journalEntity.toDomainModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun JournalEntryEntity.toDomainModel(): JournalEntry {
        return JournalEntry(
            id = id,
            type = type,
            promptId = promptId,
            title = title,
            body = body,
            quoteId = quoteId,
            exerciseId = exerciseId,
            virtues = virtues.split(",").filter { it.isNotBlank() }
                .mapNotNull { try { com.stoicjournal.core.domain.model.Virtue.valueOf(it) } catch (e: Exception) { null } },
            createdAt = Instant.ofEpochMilli(createdAtMillis),
            updatedAt = Instant.ofEpochMilli(updatedAtMillis)
        )
    }
}
