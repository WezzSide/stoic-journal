package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stoicjournal.core.domain.model.Theme

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val id: String,
    val text: String,
    val attribution: String,
    val philosopherId: String,
    val theme: Theme,
    val source: String? = null,
    val isFavorite: Boolean = false
)
