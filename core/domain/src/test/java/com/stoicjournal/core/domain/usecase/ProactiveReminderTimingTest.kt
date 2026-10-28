package com.stoicjournal.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ProactiveReminderTimingTest {
    private val zone = ZoneId.of("Europe/Amsterdam")

    @Test fun `before today's reminder schedules for today`() {
        val now = ZonedDateTime.of(2026, 10, 28, 18, 0, 0, 0, zone)
        assertEquals(Duration.ofMinutes(150), ProactiveReminderTiming.delayUntilNextReminder(now, 20, 30))
    }

    @Test fun `after today's reminder schedules for tomorrow`() {
        val now = ZonedDateTime.of(2026, 10, 28, 21, 0, 0, 0, zone)
        assertEquals(Duration.ofHours(23).plusMinutes(30), ProactiveReminderTiming.delayUntilNextReminder(now, 20, 30))
    }

    @Test fun `exactly at reminder time rolls to tomorrow`() {
        val now = ZonedDateTime.of(2026, 10, 28, 20, 30, 0, 0, zone)
        assertEquals(Duration.ofHours(24), ProactiveReminderTiming.delayUntilNextReminder(now, 20, 30))
    }

    @Test fun `DST fall-back night yields a 25 hour gap, not 24`() {
        // 2026-10-24 21:00 CEST -> 2026-10-25 20:30 CET (clocks went back 1h at 03:00)
        val now = ZonedDateTime.of(2026, 10, 24, 21, 0, 0, 0, zone)
        assertEquals(Duration.ofHours(24).plusMinutes(30), ProactiveReminderTiming.delayUntilNextReminder(now, 20, 30))
    }

    @Test fun `snooze delay is the remaining snooze window`() {
        val now = Instant.parse("2026-10-28T19:00:00Z")
        val until = now.plusSeconds(30 * 60).toEpochMilli()
        assertEquals(Duration.ofMinutes(30), ProactiveReminderTiming.delayUntilSnoozeEnds(now, until))
    }

    @Test fun `expired snooze clamps to the minimum delay`() {
        val now = Instant.parse("2026-10-28T19:00:00Z")
        assertEquals(ProactiveReminderTiming.MIN_DELAY, ProactiveReminderTiming.delayUntilSnoozeEnds(now, now.minusSeconds(60).toEpochMilli()))
    }
}
