package com.stoicjournal.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class PinAttemptPolicyTest {

    @Test
    fun `first five failures are not throttled`() {
        (0 until PinAttemptPolicy.FREE_ATTEMPTS).forEach {
            assertEquals(0L, PinAttemptPolicy.lockoutMillisAfter(it))
        }
    }

    @Test
    fun `lockout doubles from 30 seconds after the free attempts`() {
        assertEquals(30_000L, PinAttemptPolicy.lockoutMillisAfter(5))
        assertEquals(60_000L, PinAttemptPolicy.lockoutMillisAfter(6))
        assertEquals(120_000L, PinAttemptPolicy.lockoutMillisAfter(7))
        assertEquals(240_000L, PinAttemptPolicy.lockoutMillisAfter(8))
    }

    @Test
    fun `lockout is capped at fifteen minutes and never overflows`() {
        assertEquals(PinAttemptPolicy.MAX_LOCKOUT_MILLIS, PinAttemptPolicy.lockoutMillisAfter(10))
        assertEquals(PinAttemptPolicy.MAX_LOCKOUT_MILLIS, PinAttemptPolicy.lockoutMillisAfter(1_000))
        assertEquals(PinAttemptPolicy.MAX_LOCKOUT_MILLIS, PinAttemptPolicy.lockoutMillisAfter(Int.MAX_VALUE))
    }

    @Test
    fun `remaining lockout never goes negative`() {
        assertEquals(10_000L, PinAttemptPolicy.remainingLockoutMillis(lockoutUntilMillis = 20_000, nowMillis = 10_000))
        assertEquals(0L, PinAttemptPolicy.remainingLockoutMillis(lockoutUntilMillis = 20_000, nowMillis = 25_000))
        assertEquals(0L, PinAttemptPolicy.remainingLockoutMillis(lockoutUntilMillis = 0, nowMillis = 25_000))
    }
}
