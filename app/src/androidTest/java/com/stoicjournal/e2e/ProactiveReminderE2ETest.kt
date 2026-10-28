package com.stoicjournal.e2e

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import com.stoicjournal.core.domain.model.EntryType
import com.stoicjournal.core.domain.model.JournalEntry
import com.stoicjournal.core.domain.repository.JournalRepository
import com.stoicjournal.core.domain.repository.ProactiveMentorPreferencesRepository
import com.stoicjournal.core.notifications.proactive.ProactiveMentorNotificationReceiver
import com.stoicjournal.core.notifications.proactive.ProactiveMentorScheduler
import com.stoicjournal.core.notifications.proactive.ProactiveMentorWorker
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * E2E-011/012/013 without waiting for wall-clock time: runs the real ProactiveMentorWorker through
 * WorkManager's TestListenableWorkerBuilder, checks the posted notification, exercises Snooze and
 * Skip Today through the real receiver, and verifies that a completed reflection suppresses reminders.
 */
@HiltAndroidTest
class ProactiveReminderE2ETest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var prefs: ProactiveMentorPreferencesRepository
    @Inject lateinit var scheduler: ProactiveMentorScheduler
    @Inject lateinit var journalRepository: JournalRepository

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val notificationManager get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val createdEntryIds = mutableListOf<String>()

    @Before
    fun setUp() {
        hiltRule.inject()
        E2ESupport.ensureScreenOn()
        E2ESupport.ensureWorkManager(androidx.test.core.app.ApplicationProvider.getApplicationContext(), workerFactory)
        notificationManager.cancelAll()
        runBlocking {
            prefs.setProactiveMentorEnabled(true)
            prefs.setQuietHours(0, 0) // empty window: never quiet, keeps the test time-independent
            prefs.setSnoozedUntil(null)
            prefs.setSkippedTodayDate(null)
            // Other E2E classes create synthetic "QA …" entries today (GUIDED / long free-form), which the
            // eligibility engine correctly treats as "reflection completed". Remove only those synthetic rows.
            val today = LocalDate.now()
            journalRepository.getAllEntries().first()
                .filter { it.title?.startsWith("QA") == true || it.body.startsWith("QA") }
                .filter { it.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate() == today }
                .forEach { journalRepository.deleteEntry(it) }
        }
    }

    @After
    fun tearDown() {
        notificationManager.cancelAll()
        runBlocking {
            createdEntryIds.forEach { id -> journalRepository.getEntryById(id)?.let { journalRepository.deleteEntry(it) } }
            prefs.setProactiveMentorEnabled(false)
            prefs.setSnoozedUntil(null)
            prefs.setSkippedTodayDate(null)
            prefs.setQuietHours(22, 8)
        }
        scheduler.cancelPendingInvitations()
    }

    private fun runWorker(): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<ProactiveMentorWorker>(context).setWorkerFactory(workerFactory).build().doWork()
    }

    private fun invitationPosted(): Boolean =
        notificationManager.activeNotifications.any { it.id == ProactiveMentorWorker.NOTIFICATION_ID }

    private fun waitFor(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end && !condition()) Thread.sleep(100)
        assertTrue("condition not met within $timeoutMs ms", condition())
    }

    @Test
    fun eligibleWorker_postsInvitation_withReflectSnoozeSkipActions_andNoJournalText() {
        assertEquals(ListenableWorker.Result.success(), runWorker())
        waitFor { invitationPosted() }
        val sbn = notificationManager.activeNotifications.single { it.id == ProactiveMentorWorker.NOTIFICATION_ID }
        val extras = sbn.notification.extras
        assertEquals("A moment for reflection 🏛️", extras.getCharSequence("android.title").toString())
        assertFalse(extras.getCharSequence("android.text").toString().contains("QA"))
        assertEquals(listOf("Reflect Now", "Snooze 30m", "Skip Today"), sbn.notification.actions.map { it.title.toString() })
        assertNotNull(sbn.notification.contentIntent)
    }

    @Test
    fun snooze_suppressesUntilWindowEnds_andSchedulesWakeup() {
        runWorker(); waitFor { invitationPosted() }

        context.sendBroadcast(Intent(context, ProactiveMentorNotificationReceiver::class.java).setAction(ProactiveMentorNotificationReceiver.ACTION_SNOOZE))
        waitFor { runBlocking { prefs.preferences.first().snoozedUntilTimestamp } != null }
        waitFor { !invitationPosted() }
        val snoozedUntil = runBlocking { prefs.preferences.first().snoozedUntilTimestamp!! }
        assertTrue(snoozedUntil > Instant.now().toEpochMilli() + 25 * 60_000)

        waitFor {
            WorkManager.getInstance(context).getWorkInfosForUniqueWork(ProactiveMentorScheduler.SNOOZE_WORK_NAME).get()
                .any { it.state == WorkInfo.State.ENQUEUED }
        }
        // Re-running the worker inside the snooze window must not post again.
        runWorker(); Thread.sleep(500)
        assertFalse(invitationPosted())
    }

    @Test
    fun skipToday_suppressesForTheRestOfTheDay() {
        runWorker(); waitFor { invitationPosted() }
        context.sendBroadcast(Intent(context, ProactiveMentorNotificationReceiver::class.java).setAction(ProactiveMentorNotificationReceiver.ACTION_SKIP_TODAY))
        waitFor { runBlocking { prefs.preferences.first().skippedTodayDate } == LocalDate.now().toString() }
        waitFor { !invitationPosted() }
        runWorker(); Thread.sleep(500)
        assertFalse(invitationPosted())
    }

    @Test
    fun completedReflectionToday_suppressesInvitation() {
        val id = UUID.randomUUID().toString()
        createdEntryIds += id
        runBlocking {
            journalRepository.saveEntry(
                JournalEntry(id = id, type = EntryType.GUIDED, title = "QA Proactive completion", body = "QA reflection body",
                    createdAt = Instant.now(), updatedAt = Instant.now())
            )
        }
        assertEquals(ListenableWorker.Result.success(), runWorker())
        Thread.sleep(500)
        assertFalse(invitationPosted())
    }

    @Test
    fun disabledFeature_cancelsScheduledWorkAndPostsNothing() {
        scheduler.scheduleNextInvitation(runBlocking { prefs.preferences.first() })
        waitFor { WorkManager.getInstance(context).getWorkInfosForUniqueWork(ProactiveMentorScheduler.WORK_NAME).get().any { !it.state.isFinished } }

        runBlocking { prefs.setProactiveMentorEnabled(false) }
        scheduler.scheduleNextInvitation(runBlocking { prefs.preferences.first() })
        waitFor { WorkManager.getInstance(context).getWorkInfosForUniqueWork(ProactiveMentorScheduler.WORK_NAME).get().all { it.state.isFinished } }
        runWorker(); Thread.sleep(300)
        assertFalse(invitationPosted())
        assertNull(runBlocking { prefs.preferences.first().snoozedUntilTimestamp })
    }
}
