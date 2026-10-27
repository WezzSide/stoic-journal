package com.stoicjournal.core.notifications.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.notifications.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class JournalQuickCheckInReceiver : BroadcastReceiver() {

    @Inject
    lateinit var journalRepository: JournalRepository

    override fun onReceive(context: Context, intent: Intent) {
        val actionText = intent.getStringExtra(EXTRA_QUICK_ACTION) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entry = JournalEntry(
                    id = UUID.randomUUID().toString(),
                    type = EntryType.FREE_FORM,
                    title = "Quick Check-in",
                    body = "Quick status logged: $actionText",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                journalRepository.saveEntry(entry)

                showConfirmationNotification(context, actionText)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save quick check-in entry", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showConfirmationNotification(context: Context, actionText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "stoic_journal_confirm"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reflectie Opgeslagen",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Quick Check-in Opgeslagen 🏛️")
            .setContentText("Status: $actionText")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(3000)
            .build()

        notificationManager.notify(CONFIRM_NOTIFICATION_ID, notification)
    }

    companion object {
        const val EXTRA_QUICK_ACTION = "extra_quick_action"
        const val CONFIRM_NOTIFICATION_ID = 2003
        private const val TAG = "JournalQuickCheckInRx"
    }
}
