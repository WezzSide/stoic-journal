package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mentor_reflection_links",
    indices = [
        Index(value = ["conversationId", "candidateId"], unique = true)
    ]
)
data class MentorReflectionLinkEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val candidateId: String,
    val journalEntryId: String,
    val createdAt: Long,
    val savePolicy: String
)
