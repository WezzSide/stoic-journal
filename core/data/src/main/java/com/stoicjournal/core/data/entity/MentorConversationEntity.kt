package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mentor_conversations")
data class MentorConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val providerId: String?,
    val modelId: String?,
    val conversationMode: String,
    val status: String,
    val lastMessageAt: Long,
    val linkedJournalEntryId: String? = null
)
