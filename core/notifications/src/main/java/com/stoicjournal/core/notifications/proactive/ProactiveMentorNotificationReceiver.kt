package com.stoicjournal.core.notifications.proactive

import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.stoicjournal.core.domain.repository.ProactiveMentorPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class ProactiveMentorNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferencesRepository: ProactiveMentorPreferencesRepository

    @Inject
    lateinit var scheduler: ProactiveMentorScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(notificationId)

        val action = intent.action ?: return
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        when (action) {
            ACTION_SNOOZE -> {
                scope.launch {
                    try {
                        val snoozeUntil = Instant.now().plusSeconds(SNOOZE_MINUTES * 60).toEpochMilli()
                        preferencesRepository.setSnoozedUntil(snoozeUntil)
                        val prefs = preferencesRepository.preferences.first()
                        scheduler.scheduleSnoozeWakeup(prefs)
                        scheduler.scheduleNextInvitation(prefs)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to snooze proactive mentor invitation", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SKIP_TODAY -> {
                scope.launch {
                    try {
                        preferencesRepository.setSkippedTodayDate(LocalDate.now().toString())
                        preferencesRepository.setSnoozedUntil(null)
                        val prefs = preferencesRepository.preferences.first()
                        scheduler.scheduleSnoozeWakeup(prefs)
                        scheduler.scheduleNextInvitation(prefs)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to skip proactive mentor invitation", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            else -> pendingResult.finish()
        }
    }

    companion object {
        const val NOTIFICATION_ID = ProactiveMentorWorker.NOTIFICATION_ID
        const val SNOOZE_MINUTES = 30L
        private const val TAG = "ProactiveMentorReceiver"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val ACTION_SNOOZE = "com.stoicjournal.action.PROACTIVE_SNOOZE"
        const val ACTION_SKIP_TODAY = "com.stoicjournal.action.PROACTIVE_SKIP_TODAY"
    }
}
