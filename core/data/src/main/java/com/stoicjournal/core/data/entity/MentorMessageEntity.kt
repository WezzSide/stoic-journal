package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mentor_messages",
    foreignKeys = [
        ForeignKey(
            entity = MentorConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("conversationId"),
        Index("createdAt")
    ]
)
data class MentorMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val sequenceNumber: Int,
    val deliveryStatus: String,
    val requestCorrelationId: String? = null
)
