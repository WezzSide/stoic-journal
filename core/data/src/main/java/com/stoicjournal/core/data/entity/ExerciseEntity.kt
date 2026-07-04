package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.model.Theme

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val steps: String, // JSON array string
    val theme: Theme,
    val difficultyLevel: StoicismLevel = StoicismLevel.BEGINNER
)
