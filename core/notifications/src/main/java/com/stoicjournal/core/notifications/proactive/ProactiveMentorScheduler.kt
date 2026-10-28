package com.stoicjournal.core.notifications.proactive

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.stoicjournal.core.domain.repository.ProactiveMentorPreferences
import com.stoicjournal.core.domain.usecase.ProactiveReminderTiming
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the proactive Stoic Mentor invitation.
 *
 *  * A unique 24h periodic work ([WORK_NAME]) first fires at the preferred reminder time.
 *  * "Snooze" enqueues a separate unique one-time work ([SNOOZE_WORK_NAME]) at `snoozedUntil`
 *    so the invitation actually returns after the snooze window instead of the next day.
 *  * Both are cancelled when the feature is disabled. The worker re-checks eligibility on every run,
 *    so a stale enqueue can never post an unwanted notification.
 */
@Singleton
class ProactiveMentorScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    fun scheduleNextInvitation(preferences: ProactiveMentorPreferences) {
        if (!preferences.proactiveMentorEnabled) {
            cancelPendingInvitations()
            return
        }

        val delay = ProactiveReminderTiming.delayUntilNextReminder(
            ZonedDateTime.now(ZoneId.systemDefault()),
            preferences.preferredReminderHour,
            preferences.preferredReminderMinute
        )

        val workRequest = PeriodicWorkRequestBuilder<ProactiveMentorWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
            .addTag(TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    /** One-shot re-check when the snooze window ends. No-op when the feature is disabled. */
    fun scheduleSnoozeWakeup(preferences: ProactiveMentorPreferences) {
        val snoozedUntil = preferences.snoozedUntilTimestamp
        if (!preferences.proactiveMentorEnabled || snoozedUntil == null) {
            workManager.cancelUniqueWork(SNOOZE_WORK_NAME)
            return
        }
        val delay = ProactiveReminderTiming.delayUntilSnoozeEnds(Instant.now(), snoozedUntil)
        val request = OneTimeWorkRequestBuilder<ProactiveMentorWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork(SNOOZE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelPendingInvitations() {
        workManager.cancelUniqueWork(WORK_NAME)
        workManager.cancelUniqueWork(SNOOZE_WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "stoic_mentor_proactive_invitation"
        const val SNOOZE_WORK_NAME = "stoic_mentor_proactive_snooze"
        const val TAG = "stoic_mentor_proactive"
    }
}
