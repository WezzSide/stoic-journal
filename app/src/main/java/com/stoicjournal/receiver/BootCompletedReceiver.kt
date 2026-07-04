package com.stoicjournal.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.stoicjournal.core.common.di.ApplicationScope
import com.stoicjournal.core.domain.repository.PreferencesRepository
import com.stoicjournal.core.notifications.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        applicationScope.launch {
            try {
                val prefs = preferencesRepository.preferences.first()
                if (prefs.reminderEnabled) {
                    reminderScheduler.schedule(prefs.reminderHour, prefs.reminderMinute)
                    Log.d(TAG, "Rescheduled daily reminder for ${prefs.reminderHour}:${prefs.reminderMinute}")
                } else {
                    Log.d(TAG, "Reminders disabled; not rescheduling after boot")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule reminder after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
