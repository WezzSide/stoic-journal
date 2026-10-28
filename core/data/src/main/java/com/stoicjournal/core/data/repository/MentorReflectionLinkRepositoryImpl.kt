package com.stoicjournal.core.data.repository

import com.stoicjournal.core.data.dao.MentorReflectionLinkDao
import com.stoicjournal.core.data.entity.MentorReflectionLinkEntity
import com.stoicjournal.core.domain.model.MentorReflectionLink
import com.stoicjournal.core.domain.repository.MentorReflectionLinkRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MentorReflectionLinkRepositoryImpl @Inject constructor(
    private val linkDao: MentorReflectionLinkDao
) : MentorReflectionLinkRepository {

    override suspend fun getLink(conversationId: String, candidateId: String): MentorReflectionLink? {
        val entity = linkDao.getLink(conversationId, candidateId) ?: return null
        return MentorReflectionLink(
            id = entity.id,
            conversationId = entity.conversationId,
            candidateId = entity.candidateId,
            journalEntryId = entity.journalEntryId,
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            savePolicy = entity.savePolicy
        )
    }

    override suspend fun saveLink(link: MentorReflectionLink) {
        val entity = MentorReflectionLinkEntity(
            id = link.id,
            conversationId = link.conversationId,
            candidateId = link.candidateId,
            journalEntryId = link.journalEntryId,
            createdAt = link.createdAt.toEpochMilli(),
            savePolicy = link.savePolicy
        )
        linkDao.insertLink(entity)
    }
}
