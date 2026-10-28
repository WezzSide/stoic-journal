package com.stoicjournal.core.domain.repository

import com.stoicjournal.core.domain.model.MentorReflectionLink

interface MentorReflectionLinkRepository {
    suspend fun getLink(conversationId: String, candidateId: String): MentorReflectionLink?
    suspend fun saveLink(link: MentorReflectionLink)
}
