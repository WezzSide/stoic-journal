package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mentor_memories")
data class MentorMemoryEntity(
    @PrimaryKey val id: String,
    val category: String,
    val content: String,
    val sourceEntryIds: String,
    val sourceConversationIds: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastReviewedAt: Long?,
    val status: String,
    val userApproved: Boolean
)
