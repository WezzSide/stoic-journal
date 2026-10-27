package com.stoicjournal.core.domain.util

import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.model.Streak
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object StreakCalculator {

    fun calculate(entries: List<JournalEntry>, allowFreeze: Boolean = false): Streak {
        if (entries.isEmpty()) return Streak(currentCount = 0, lastEntryDate = null)
        val dates = entries
            .map { it.createdAt.atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sortedDescending()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)
        val mostRecent = dates.first()

        val isValidStart = if (allowFreeze) {
            mostRecent == today || mostRecent == yesterday || mostRecent == twoDaysAgo
        } else {
            mostRecent == today || mostRecent == yesterday
        }

        if (!isValidStart) {
            return Streak(currentCount = 0, lastEntryDate = mostRecent)
        }

        var streak = 1
        var current = mostRecent
        var freezeUsed = false

        for (date in dates.drop(1)) {
            val diffDays = ChronoUnit.DAYS.between(date, current)
            if (diffDays == 1L) {
                streak++
                current = date
            } else if (diffDays == 2L && allowFreeze && !freezeUsed) {
                freezeUsed = true
                streak++
                current = date
            } else {
                break
            }
        }
        return Streak(currentCount = streak, lastEntryDate = mostRecent)
    }
}
