package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "philosophers")
data class PhilosopherEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lived: String,
    val shortBio: String
)
