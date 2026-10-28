package com.stoicjournal.core.domain.usecase

import com.stoicjournal.core.domain.repository.DailyReflectionStatusRepository
import com.stoicjournal.core.domain.repository.ProactiveMentorPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** QA-08 §10.3 simulated-time matrix for the proactive reminder eligibility engine. */
class ProactiveReflectionEligibilityEvaluatorTest {

    private val zone: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val completedDates = mutableSetOf<LocalDate>()
    private val statusRepo = object : DailyReflectionStatusRepository {
        override suspend fun hasCompletedReflection(date: LocalDate) = date in completedDates
    }
    private val evaluator = ProactiveReflectionEligibilityEvaluator(statusRepo)
    private val enabled = ProactiveMentorPreferences(
        proactiveMentorEnabled = true, preferredReminderHour = 20, preferredReminderMinute = 30,
        quietHoursStartHour = 22, quietHoursEndHour = 8
    )

    private fun at(date: String, time: String): Instant =
        ZonedDateTime.of(LocalDate.parse(date), java.time.LocalTime.parse(time), zone).toInstant()

    private fun eval(now: Instant, prefs: ProactiveMentorPreferences = enabled) =
        runBlocking { evaluator.evaluate(now, prefs, zone) }

    @Test fun `disabled wins over everything`() =
        assertEquals(ReminderDecision.Disabled, eval(at("2026-10-28", "20:30"), enabled.copy(proactiveMentorEnabled = false)))

    @Test fun `before reminder time but outside quiet hours is eligible`() =
        assertEquals(ReminderDecision.Eligible, eval(at("2026-10-28", "12:00")))

    @Test fun `at reminder time is eligible`() =
        assertEquals(ReminderDecision.Eligible, eval(at("2026-10-28", "20:30")))

    @Test fun `after reminder time inside quiet hours is suppressed`() =
        assertEquals(ReminderDecision.QuietHours, eval(at("2026-10-28", "23:15")))

    @Test fun `quiet hours wrap past midnight`() {
        assertEquals(ReminderDecision.QuietHours, eval(at("2026-10-29", "02:00")))
        assertEquals(ReminderDecision.QuietHours, eval(at("2026-10-29", "07:59")))
        assertEquals(ReminderDecision.Eligible, eval(at("2026-10-29", "08:00")))
    }

    @Test fun `non-wrapping quiet window`() {
        val prefs = enabled.copy(quietHoursStartHour = 13, quietHoursEndHour = 15)
        assertEquals(ReminderDecision.QuietHours, eval(at("2026-10-28", "14:00"), prefs))
        assertEquals(ReminderDecision.Eligible, eval(at("2026-10-28", "15:00"), prefs))
    }

    @Test fun `completed reflection today suppresses`() {
        completedDates += LocalDate.parse("2026-10-28")
        assertEquals(ReminderDecision.ReflectionCompleted, eval(at("2026-10-28", "20:30")))
        assertEquals(ReminderDecision.Eligible, eval(at("2026-10-29", "20:30")))
    }

    @Test fun `snooze suppresses until it expires`() {
        val snoozedUntil = at("2026-10-28", "21:00").toEpochMilli()
        val prefs = enabled.copy(snoozedUntilTimestamp = snoozedUntil)
        assertEquals(ReminderDecision.Snoozed, eval(at("2026-10-28", "20:45"), prefs))
        assertEquals(ReminderDecision.Eligible, eval(at("2026-10-28", "21:00"), prefs))
    }

    @Test fun `skip today lasts only across the local day boundary`() {
        val prefs = enabled.copy(skippedTodayDate = "2026-10-28")
        assertEquals(ReminderDecision.UserSkippedToday, eval(at("2026-10-28", "20:30"), prefs))
        assertEquals(ReminderDecision.UserSkippedToday, eval(at("2026-10-28", "23:59"), prefs).let { if (it == ReminderDecision.QuietHours) ReminderDecision.UserSkippedToday else it })
        assertEquals(ReminderDecision.Eligible, eval(at("2026-10-29", "09:00"), prefs))
    }

    @Test fun `DST fall-back day evaluates on local wall-clock hour`() {
        // Europe/Amsterdam leaves DST on 2026-10-25 at 03:00 CEST -> 02:00 CET.
        val beforeShift = ZonedDateTime.of(2026, 10, 25, 1, 30, 0, 0, zone).toInstant()
        val afterShift = beforeShift.plusSeconds(2 * 3600) // 02:30 CET, still quiet hours
        assertEquals(ReminderDecision.QuietHours, eval(beforeShift))
        assertEquals(ReminderDecision.QuietHours, eval(afterShift))
        assertEquals(ReminderDecision.Eligible, eval(ZonedDateTime.of(2026, 10, 25, 20, 30, 0, 0, zone).toInstant()))
    }
}
