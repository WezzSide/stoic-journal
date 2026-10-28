package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.repository.DailyReflectionStatusRepository
import com.stoicjournal.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

import com.stoicjournal.core.domain.model.EntryType

@Singleton
class DailyReflectionStatusRepositoryImpl @Inject constructor(
    private val journalRepository: JournalRepository
) : DailyReflectionStatusRepository {

    override suspend fun hasCompletedReflection(date: LocalDate): Boolean {
        val entries = journalRepository.getAllEntries().first()
        return entries.any { entry ->
            val entryDate = entry.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
            val isMeaningful = when (entry.type) {
                EntryType.FREE_FORM -> entry.body.length > 50
                EntryType.GUIDED -> true
                else -> false
            }
            entryDate == date && isMeaningful
        }
    }
}
