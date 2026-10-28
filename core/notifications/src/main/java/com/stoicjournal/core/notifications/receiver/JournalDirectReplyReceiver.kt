package com.stoicjournal.core.notifications.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
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
class JournalDirectReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var journalRepository: JournalRepository

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInputResults = RemoteInput.getResultsFromIntent(intent) ?: return
        val replyText = remoteInputResults.getCharSequence(KEY_TEXT_REPLY)?.toString()

        if (replyText.isNullOrBlank()) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isMorning = intent.getBooleanExtra("is_morning", false)
                val title = if (isMorning) "Ochtend-intentie" else "Avond-reflectie"

                val entry = JournalEntry(
                    id = UUID.randomUUID().toString(),
                    type = EntryType.FREE_FORM,
                    title = title,
                    body = replyText,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                journalRepository.saveEntry(entry)

                showConfirmationNotification(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save direct reply journal entry", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showConfirmationNotification(context: Context) {
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
            .setContentTitle("Reflectie Opgeslagen 🏛️")
            .setContentText("Je dagboek is bijgewerkt.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(3000)
            .build()

        notificationManager.notify(CONFIRM_NOTIFICATION_ID, notification)
    }

    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val CONFIRM_NOTIFICATION_ID = 2002
        private const val TAG = "JournalDirectReplyRx"
    }
}
