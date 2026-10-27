package com.stoicjournal.core.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun schedule(hour: Int, minute: Int) {
        scheduleEvening(hour, minute)
    }

    fun scheduleMorning(hour: Int = 8, minute: Int = 0) {
        scheduleInternal(DailyReminderWorker.WORK_NAME_MORNING, hour, minute, isMorning = true)
    }

    fun scheduleEvening(hour: Int = 21, minute: Int = 0) {
        scheduleInternal(DailyReminderWorker.WORK_NAME_EVENING, hour, minute, isMorning = false)
    }

    private fun scheduleInternal(workName: String, hour: Int, minute: Int, isMorning: Boolean) {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (target.isBefore(now)) {
            target = target.plusDays(1)
        }
        val delay = Duration.between(now, target)

        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delay)
            .setInputData(
                workDataOf(
                    "hour" to hour,
                    "minute" to minute,
                    "is_morning" to isMorning
                )
            )
            .addTag(workName)
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(DailyReminderWorker.WORK_NAME)
        workManager.cancelUniqueWork(DailyReminderWorker.WORK_NAME_MORNING)
        workManager.cancelUniqueWork(DailyReminderWorker.WORK_NAME_EVENING)
    }

    fun cancelMorning() {
        workManager.cancelUniqueWork(DailyReminderWorker.WORK_NAME_MORNING)
    }

    fun cancelEvening() {
        workManager.cancelUniqueWork(DailyReminderWorker.WORK_NAME_EVENING)
    }
}
