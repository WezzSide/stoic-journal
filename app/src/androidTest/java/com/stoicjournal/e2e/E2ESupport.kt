package com.stoicjournal.e2e

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.stoicjournal.core.domain.repository.PreferencesRepository
import kotlinx.coroutines.runBlocking

/** Shared helpers for the on-device regression suite (QA-14). All data is synthetic ("QA …"). */
object E2ESupport {
    const val APP_PACKAGE = "com.stoicjournal"

    /**
     * The Hilt test application does not implement Configuration.Provider and the manifest removes the
     * default WorkManager initializer, so tests initialise a test WorkManager wired to the real
     * HiltWorkerFactory (idempotent).
     */
    fun ensureWorkManager(context: android.content.Context, workerFactory: androidx.hilt.work.HiltWorkerFactory) {
        try {
            androidx.work.WorkManager.getInstance(context)
        } catch (_: IllegalStateException) {
            androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(
                context,
                androidx.work.Configuration.Builder().setWorkerFactory(workerFactory).build()
            )
        }
    }

    /** The physical test device dozes and shows the keyguard after a short timeout; wake it before each test. */
    fun ensureScreenOn() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        if (!device.isScreenOn) device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        device.waitForIdle(1_000)
    }

    /**
     * Never route a test request to a real provider for which the device holds a credential: that would be
     * a live, billable call with the owner's key. Returns a real provider id with NO stored credential, or
     * null when every real provider is configured (callers then skip via Assume).
     */
    fun keylessRealProviderId(credentials: com.stoicjournal.core.domain.repository.SecureCredentialRepository): String? = runBlocking {
        listOf("claude", "gemini", "openai").firstOrNull { !credentials.hasCredential(it) }
    }

    fun markOnboardingComplete(prefs: PreferencesRepository) = runBlocking {
        prefs.setOnboardingComplete(true)
        prefs.setPinLockEnabled(false)
        prefs.setBiometricLockEnabled(false)
    }

    /**
     * On this personal Pixel the Google autofill service occasionally opens a Play-services (Wallet)
     * activity on top of the app right after text input, which pauses the activity under test and
     * makes the Compose hierarchy disappear. The real app is unaffected (the user simply dismisses the
     * sheet); for a deterministic suite we press Back whenever a foreign package owns the foreground.
     */
    fun dismissForeignOverlay(): Boolean {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val top = device.currentPackageName
        if (top != null && top != APP_PACKAGE) {
            device.pressBack()
            device.waitForIdle(2_000)
            return true
        }
        return false
    }
}

fun ComposeTestRule.waitForText(text: String, timeoutMillis: Long = 10_000, substring: Boolean = false) {
    waitUntil(timeoutMillis) {
        try {
            onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        } catch (e: IllegalStateException) {
            if (!E2ESupport.dismissForeignOverlay()) throw e
            false
        }
    }
}

fun ComposeTestRule.waitForTextGone(text: String, timeoutMillis: Long = 10_000) {
    waitUntil(timeoutMillis) {
        try {
            onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        } catch (e: IllegalStateException) {
            if (!E2ESupport.dismissForeignOverlay()) throw e
            false
        }
    }
}
