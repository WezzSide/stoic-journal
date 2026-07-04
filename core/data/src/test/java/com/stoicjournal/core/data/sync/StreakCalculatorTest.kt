package com.stoicjournal.core.data.sync

import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StreakCalculatorTest {

    @Test
    fun `empty entries returns zero streak`() {
        val result = StreakCalculator.calculate(emptyList())
        assertEquals(0, result.currentCount)
        assertEquals(null, result.lastEntryDate)
    }

    @Test
    fun `consecutive days returns correct streak`() {
        val entries = (0L..2L).map { daysAgo ->
            entryOn(LocalDate.now().minusDays(daysAgo))
        }
        val result = StreakCalculator.calculate(entries)
        assertEquals(3, result.currentCount)
    }

    @Test
    fun `gap breaks streak`() {
        val entries = listOf(
            entryOn(LocalDate.now()),
            entryOn(LocalDate.now().minusDays(2))
        )
        val result = StreakCalculator.calculate(entries)
        assertEquals(1, result.currentCount)
    }

    private fun entryOn(date: LocalDate): JournalEntry {
        val instant = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        return JournalEntry(
            id = instant.toString(),
            type = EntryType.GUIDED,
            body = "",
            createdAt = instant,
            updatedAt = instant
        )
    }
}
