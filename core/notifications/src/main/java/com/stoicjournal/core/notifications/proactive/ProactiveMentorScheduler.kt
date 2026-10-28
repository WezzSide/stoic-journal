package com.stoicjournal.core.notifications.proactive

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.stoicjournal.core.domain.repository.ProactiveMentorPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProactiveMentorScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleNextInvitation(preferences: ProactiveMentorPreferences) {
        if (!preferences.proactiveMentorEnabled) {
            cancelPendingInvitations()
            return
        }

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        var targetTime = ZonedDateTime.of(
            LocalDate.now(),
            LocalTime.of(preferences.preferredReminderHour, preferences.preferredReminderMinute),
            ZoneId.systemDefault()
        )

        if (now.isAfter(targetTime)) {
            targetTime = targetTime.plusDays(1)
        }

        val initialDelay = Duration.between(now, targetTime).toMinutes().coerceAtLeast(1)

        val workRequest = PeriodicWorkRequestBuilder<ProactiveMentorWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelPendingInvitations() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "stoic_mentor_proactive_invitation"
    }
}
