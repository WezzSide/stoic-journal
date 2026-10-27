package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_reflections")
data class AiReflectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val insight: String,
    val interpretation: String,
    val reflectiveQuestion: String,
    val actionableStep: String,
    val quote: String?,
    val principle: String?,
    val timestamp: Long,
    val theme: String
)
