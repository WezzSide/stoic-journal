package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.repository.DailyReflectionStatusRepository
import com.stoicjournal.core.domain.repository.ProactiveMentorPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ReminderDecision {
    data object Eligible : ReminderDecision
    data object Disabled : ReminderDecision
    data object ReflectionCompleted : ReminderDecision
    data object QuietHours : ReminderDecision
    data object Snoozed : ReminderDecision
    data object UserSkippedToday : ReminderDecision
}

@Singleton
class ProactiveReflectionEligibilityEvaluator @Inject constructor(
    private val reflectionStatusRepository: DailyReflectionStatusRepository
) {
    suspend fun evaluate(
        now: Instant,
        preferences: ProactiveMentorPreferences,
        timeZone: ZoneId = ZoneId.systemDefault()
    ): ReminderDecision {
        if (!preferences.proactiveMentorEnabled) {
            return ReminderDecision.Disabled
        }

        val zdt = ZonedDateTime.ofInstant(now, timeZone)
        val today = zdt.toLocalDate()

        if (preferences.skippedTodayDate == today.toString()) {
            return ReminderDecision.UserSkippedToday
        }

        val snoozedUntil = preferences.snoozedUntilTimestamp
        if (snoozedUntil != null && now.toEpochMilli() < snoozedUntil) {
            return ReminderDecision.Snoozed
        }

        val hour = zdt.hour
        val quietStart = preferences.quietHoursStartHour
        val quietEnd = preferences.quietHoursEndHour
        val isQuiet = if (quietStart > quietEnd) {
            hour >= quietStart || hour < quietEnd
        } else {
            hour in quietStart..<quietEnd
        }
        if (isQuiet) {
            return ReminderDecision.QuietHours
        }

        val hasCompleted = reflectionStatusRepository.hasCompletedReflection(today)
        if (hasCompleted) {
            return ReminderDecision.ReflectionCompleted
        }

        return ReminderDecision.Eligible
    }
}
