package com.stoicjournal.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stoicjournal.core.domain.repository.PreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Worker that displays the daily reminder notification and reschedules itself for the next day.
 *
 * NOTE (Task 7): The Application class in the :app module must implement
 * [androidx.work.Configuration.Provider] and return a [androidx.hilt.work.HiltWorkerFactory]
 * so that Hilt can inject this worker. Example:
 *
 * ```
 * @dagger.hilt.android.HiltAndroidApp
 * class StoicJournalApplication : Application(), Configuration.Provider {
 *     @Inject lateinit var workerFactory: HiltWorkerFactory
 *
 *     override fun getWorkManagerConfiguration(): Configuration {
 *         return Configuration.Builder().setWorkerFactory(workerFactory).build()
 *     }
 * }
 * ```
 */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminderScheduler: ReminderScheduler,
    private val preferencesRepository: PreferencesRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(R.string.reminder_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.reminder_title))
            .setContentText(applicationContext.getString(R.string.reminder_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        val prefs = preferencesRepository.preferences.first()
        if (prefs.reminderEnabled) {
            val hour = inputData.getInt("hour", DEFAULT_HOUR)
            val minute = inputData.getInt("minute", DEFAULT_MINUTE)
            reminderScheduler.schedule(hour, minute)
        } else {
            Log.d(TAG, "Reminders disabled; skipping next-day reschedule")
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "daily_reminder"
        const val CHANNEL_ID = "stoic_journal_reminder"
        const val NOTIFICATION_ID = 1001

        private const val DEFAULT_HOUR = 20
        private const val DEFAULT_MINUTE = 0
        private const val TAG = "DailyReminderWorker"
    }
}
