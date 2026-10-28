package com.stoicjournal.core.notifications.proactive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stoicjournal.core.domain.repository.ProactiveMentorPreferencesRepository
import com.stoicjournal.core.domain.usecase.ProactiveReflectionEligibilityEvaluator
import com.stoicjournal.core.domain.usecase.ReminderDecision
import com.stoicjournal.core.common.notifications.NotificationRoutes
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant

@HiltWorker
class ProactiveMentorWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val preferencesRepository: ProactiveMentorPreferencesRepository,
    private val eligibilityEvaluator: ProactiveReflectionEligibilityEvaluator
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val preferences = preferencesRepository.preferences.first()
        val decision = eligibilityEvaluator.evaluate(Instant.now(), preferences)

        if (decision is ReminderDecision.Eligible) {
            postInvitationNotification()
        }

        return Result.success()
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun postInvitationNotification() {
        createNotificationChannel()

        val mainActivityClass = Class.forName("com.stoicjournal.MainActivity")
        val contentIntent = Intent(context, mainActivityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationRoutes.EXTRA_ROUTE, NotificationRoutes.ROUTE_MENTOR_CHAT)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ProactiveMentorNotificationReceiver::class.java).apply {
            action = ProactiveMentorNotificationReceiver.ACTION_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(context, ProactiveMentorNotificationReceiver::class.java).apply {
            action = ProactiveMentorNotificationReceiver.ACTION_SKIP_TODAY
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("A moment for reflection 🏛️")
            .setContentText("Would you like to reflect on your day with Stoic Mentor?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // static copy only, never journal/AI text
            .setAutoCancel(true)
            .addAction(0, "Reflect Now", contentPendingIntent)
            .addAction(0, "Snooze 30m", snoozePendingIntent)
            .addAction(0, "Skip Today", skipPendingIntent)
            .build()

        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < 33) {
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {}
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Stoic Mentor Invitations"
            val descriptionText = "Proactive reflection reminders from Stoic Mentor"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "stoic_mentor_reflection"
        // 2002 collided with JournalDirectReplyReceiver.CONFIRM_NOTIFICATION_ID.
        const val NOTIFICATION_ID = 2003
    }
}
