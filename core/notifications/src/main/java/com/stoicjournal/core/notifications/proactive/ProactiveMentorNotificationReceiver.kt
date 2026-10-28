package com.stoicjournal.core.notifications.proactive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.stoicjournal.core.domain.repository.ProactiveMentorPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class ProactiveMentorNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferencesRepository: ProactiveMentorPreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(notificationId)

        val action = intent.action ?: return
        val scope = CoroutineScope(Dispatchers.IO)

        when (action) {
            ACTION_SNOOZE -> {
                scope.launch {
                    val snooze30Min = Instant.now().plusSeconds(30 * 60).toEpochMilli()
                    preferencesRepository.setSnoozedUntil(snooze30Min)
                }
            }
            ACTION_SKIP_TODAY -> {
                scope.launch {
                    preferencesRepository.setSkippedTodayDate(LocalDate.now().toString())
                }
            }
        }
    }

    companion object {
        const val NOTIFICATION_ID = 2002
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val ACTION_SNOOZE = "com.stoicjournal.action.PROACTIVE_SNOOZE"
        const val ACTION_SKIP_TODAY = "com.stoicjournal.action.PROACTIVE_SKIP_TODAY"
    }
}
