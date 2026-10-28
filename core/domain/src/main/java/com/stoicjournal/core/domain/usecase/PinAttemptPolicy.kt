package com.stoicjournal.core.domain.usecase

/**
 * Brute-force throttling for the 4-digit app-lock PIN (D-24).
 *
 * Up to [FREE_ATTEMPTS] - 1 consecutive wrong PINs are unthrottled; the [FREE_ATTEMPTS]th and every
 * further failure lock the PIN pad for an exponentially growing period (30 s, 60 s, 2 min … capped at [MAX_LOCKOUT_MILLIS]). The counter and
 * lock-out deadline are persisted, so killing the app does not reset them.
 */
object PinAttemptPolicy {
    const val FREE_ATTEMPTS = 5
    const val BASE_LOCKOUT_MILLIS = 30_000L
    const val MAX_LOCKOUT_MILLIS = 15 * 60_000L

    /** Lock-out duration that applies after [failedAttempts] consecutive wrong PINs. */
    fun lockoutMillisAfter(failedAttempts: Int): Long {
        if (failedAttempts < FREE_ATTEMPTS) return 0L
        val exponent = (failedAttempts - FREE_ATTEMPTS).coerceAtMost(20)
        return (BASE_LOCKOUT_MILLIS shl exponent).coerceAtMost(MAX_LOCKOUT_MILLIS)
    }

    /** Remaining lock-out at [nowMillis]; 0 when a PIN may be entered. */
    fun remainingLockoutMillis(lockoutUntilMillis: Long, nowMillis: Long): Long =
        (lockoutUntilMillis - nowMillis).coerceAtLeast(0L)
}
