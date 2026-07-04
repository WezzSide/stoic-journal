package com.stoicjournal.core.data.sync

import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.model.Streak
import java.time.LocalDate
import java.time.ZoneId

object StreakCalculator {

    fun calculate(entries: List<JournalEntry>): Streak {
        if (entries.isEmpty()) return Streak(currentCount = 0, lastEntryDate = null)
        val dates = entries
            .map { it.createdAt.atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sortedDescending()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val mostRecent = dates.first()
        if (mostRecent != today && mostRecent != yesterday) {
            return Streak(currentCount = 0, lastEntryDate = mostRecent)
        }
        var streak = 1
        var current = mostRecent
        for (date in dates.drop(1)) {
            if (date == current.minusDays(1)) {
                streak++
                current = date
            } else {
                break
            }
        }
        return Streak(currentCount = streak, lastEntryDate = mostRecent)
    }
}
