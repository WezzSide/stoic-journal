package com.stoicjournal.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stoicjournal.core.domain.model.EntryType

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val type: EntryType,
    val promptId: String? = null,
    val title: String? = null,
    val body: String,
    val quoteId: String? = null,
    val exerciseId: String? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
