## Task 6: Implement daily reminder notification

**Files:**
- Create: `core/notifications/src/main/java/com/stoicjournal/core/notifications/DailyReminderWorker.kt`
- Create: `core/notifications/src/main/java/com/stoicjournal/core/notifications/ReminderScheduler.kt`
- Create: `core/notifications/build.gradle.kts`

**Interfaces:**
- Consumes: `UserPreferences` from `core:domain`.
- Produces: A WorkManager worker that shows a daily notification; a scheduler that cancels and re-enqueues based on preference changes.

- [ ] **Step 1: Write `core/notifications/build.gradle.kts`**

```kotlin
plugins {
    id("stoic.android.library")
    id("stoic.android.hilt")
}

android {
    namespace = "com.stoicjournal.core.notifications"
    buildFeatures {
        compose = false
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

- [ ] **Step 2: Write `DailyReminderWorker.kt`**

```kotlin
package com.stoicjournal.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminderScheduler: ReminderScheduler
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

        val hour = inputData.getInt("hour", 20)
        val minute = inputData.getInt("minute", 0)
        reminderScheduler.schedule(hour, minute)

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "daily_reminder"
        const val CHANNEL_ID = "stoic_journal_reminder"
        const val NOTIFICATION_ID = 1001
    }
}
```

- [ ] **Step 3: Create notification string resources**

Create `core/notifications/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="reminder_channel_name">Daily Reminders</string>
    <string name="reminder_channel_description">Reminders to reflect with Stoic Journal.</string>
    <string name="reminder_title">Time to reflect</string>
    <string name="reminder_body">A few minutes of Stoic reflection can change the day.</string>
</resources>
```

- [ ] **Step 4: Add a placeholder notification icon drawable**

Create `core/notifications/src/main/res/drawable/ic_notification.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8z"/>
</vector>
```

- [ ] **Step 5: Write `ReminderScheduler.kt`**

```kotlin
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
        val now = LocalDateTime.now(ZoneId.systemDefault())
        var target = now.withHour(hour).withMinute(minute).withSecond(0)
        if (target.isBefore(now)) {
            target = target.plusDays(1)
        }
        val delay = Duration.between(now, target)

        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delay)
            .setInputData(workDataOf("hour" to hour, "minute" to minute))
            .addTag(DailyReminderWorker.WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            DailyReminderWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(DailyReminderWorker.WORK_NAME)
    }
}
```

Note: To make the reminder repeat daily, configure the worker to reschedule itself after firing, or use `WorkManager` chained scheduling. For MVP, `ReminderScheduler.schedule()` is called whenever preferences change; the worker itself can call `ReminderScheduler.schedule()` again after showing the notification. Add an `Application`/`Worker` invocation in Task 7.

- [ ] **Step 6: Configure Hilt Worker factory in app**

In `app/src/main/java/com/stoicjournal/StoicJournalApplication.kt`:

```kotlin
package com.stoicjournal

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StoicJournalApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
```

- [ ] **Step 7: Build notifications module**

Run: `./gradlew :core:notifications:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add core/notifications/
git commit -m "feat(notifications): add daily reminder worker and scheduler"
```

---

