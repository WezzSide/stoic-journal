package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mentor_sessions",
    foreignKeys = [
        ForeignKey(
            entity = MentorConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("conversationId")
    ]
)
data class MentorSessionEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val status: String,
    val reflectionCandidateId: String? = null
)
