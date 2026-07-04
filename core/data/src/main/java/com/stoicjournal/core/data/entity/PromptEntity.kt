package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stoicjournal.core.domain.model.StoicismLevel
import com.stoicjournal.core.domain.model.Theme

@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey val id: String,
    val text: String,
    val theme: Theme,
    val philosopherId: String? = null,
    val difficultyLevel: StoicismLevel = StoicismLevel.BEGINNER
)
