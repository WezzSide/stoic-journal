package com.stoicjournal.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.notifications.receiver.JournalDirectReplyReceiver
import com.stoicjournal.core.notifications.receiver.JournalQuickCheckInReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminderScheduler: ReminderScheduler,
    private val preferencesRepository: PreferencesRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = preferencesRepository.preferences.first()
        val isMorning = inputData.getBoolean("is_morning", false)

        val isEnabled = if (isMorning) prefs.morningReminderEnabled else (prefs.eveningReminderEnabled || prefs.reminderEnabled)
        if (!isEnabled) {
            Log.d(TAG, "Reminder (isMorning=$isMorning) disabled; skipping")
            return Result.success()
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = applicationContext.getString(R.string.reminder_channel_description)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (isMorning) "Ochtend-intentie (Premeditatio Malorum) 🌅" else "Avond-reflectie (Seneca) 🌙"
        val promptText = if (isMorning) {
            "Welke obstakels kun je vandaag tegenkomen en hoe reageer je daar kalm op?"
        } else {
            "Wat ging er vandaag goed en wat kon je beter doen?"
        }

        // Direct Reply RemoteInput
        val remoteInput = RemoteInput.Builder(JournalDirectReplyReceiver.KEY_TEXT_REPLY)
            .setLabel("Schrijf direct je reflectie...")
            .build()

        val replyIntent = Intent(applicationContext, JournalDirectReplyReceiver::class.java).apply {
            putExtra("is_morning", isMorning)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            if (isMorning) 101 else 102,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            "Direct Reageren",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // Quick Check-in Actions
        val calmIntent = Intent(applicationContext, JournalQuickCheckInReceiver::class.java).apply {
            putExtra(JournalQuickCheckInReceiver.EXTRA_QUICK_ACTION, "🧘 Kalm & Beheerst")
        }
        val calmPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            201,
            calmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calmAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            "🧘 Kalm",
            calmPendingIntent
        ).build()

        val courageIntent = Intent(applicationContext, JournalQuickCheckInReceiver::class.java).apply {
            putExtra(JournalQuickCheckInReceiver.EXTRA_QUICK_ACTION, "🦁 Moed Getoond")
        }
        val couragePendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            202,
            courageIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val courageAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            "🦁 Moed",
            couragePendingIntent
        ).build()

        val notificationId = if (isMorning) MORNING_NOTIFICATION_ID else EVENING_NOTIFICATION_ID

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(promptText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(promptText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addAction(replyAction)
            .addAction(calmAction)
            .addAction(courageAction)
            .build()

        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasNotificationPermission) {
            try {
                notificationManager.notify(notificationId, notification)
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException showing reminder notification", e)
            }
        } else {
            Log.d(TAG, "Notification permission denied; skipping reminder notification")
        }

        val hour = inputData.getInt("hour", if (isMorning) DEFAULT_MORNING_HOUR else DEFAULT_EVENING_HOUR)
        val minute = inputData.getInt("minute", DEFAULT_MINUTE)
        if (isMorning) {
            reminderScheduler.scheduleMorning(hour, minute)
        } else {
            reminderScheduler.scheduleEvening(hour, minute)
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "daily_reminder"
        const val WORK_NAME_MORNING = "daily_reminder_morning"
        const val WORK_NAME_EVENING = "daily_reminder_evening"
        const val CHANNEL_ID = "stoic_journal_reminder"
        const val MORNING_NOTIFICATION_ID = 1001
        const val EVENING_NOTIFICATION_ID = 1002

        private const val DEFAULT_MORNING_HOUR = 8
        private const val DEFAULT_EVENING_HOUR = 21
        private const val DEFAULT_MINUTE = 0
        private const val TAG = "DailyReminderWorker"
    }
}
