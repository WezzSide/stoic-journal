package com.stoicjournal.core.domain.usecase

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Pure scheduling arithmetic for the proactive Stoic Mentor reminder so it can be unit tested
 * without WorkManager. All results are clamped to at least one minute because WorkManager
 * initial delays of zero fire immediately.
 */
object ProactiveReminderTiming {
    val MIN_DELAY: Duration = Duration.ofMinutes(1)

    /** Delay from [now] until the next occurrence of [hour]:[minute] in [now]'s zone (today or tomorrow). */
    fun delayUntilNextReminder(now: ZonedDateTime, hour: Int, minute: Int): Duration {
        var target = now.with(LocalTime.of(hour, minute, 0, 0))
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return clamp(Duration.between(now, target))
    }

    /** Delay from [now] until [snoozedUntilEpochMillis]; never shorter than [MIN_DELAY]. */
    fun delayUntilSnoozeEnds(now: Instant, snoozedUntilEpochMillis: Long): Duration {
        return clamp(Duration.between(now, Instant.ofEpochMilli(snoozedUntilEpochMillis)))
    }

    private fun clamp(duration: Duration): Duration =
        if (duration < MIN_DELAY) MIN_DELAY else duration
}
