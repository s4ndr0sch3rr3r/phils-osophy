package com.example.phils_osophy.data.remote

import java.io.IOException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class TmdbClientRetryDelayTest {
    private fun delay(retryCount: Int, header: String?): Long =
        invokeTmdbFunction("tmdbRetryDelayMillis", retryCount, header) as Long

    @Test fun exponentialBackoffStartsAtOneSecondAndCapsAtEightSeconds() {
        listOf(1_000L, 2_000L, 4_000L, 8_000L, 8_000L).forEachIndexed { retry, expected ->
            assertEquals(expected, delay(retry, null))
        }
    }

    @Test fun numericRetryAfterOverridesBackoffAndIsClamped() {
        for (retry in 0..3) {
            assertEquals(0L, delay(retry, "0"))
            assertEquals(2_000L, delay(retry, " 2 "))
            assertEquals(0L, delay(retry, "-1"))
            assertEquals(30_000L, delay(retry, "30"))
            assertEquals(30_000L, delay(retry, "120"))
        }
    }

    @Test fun invalidOrDateRetryAfterFallsBackToExponentialBackoff() {
        for (header in listOf("", "invalid", "1.5", "Wed, 21 Oct 2015 07:28:00 GMT")) {
            for (retry in 0..3) {
                assertEquals(delay(retry, null), delay(retry, header))
            }
        }
    }

    @Test fun zeroDelayStillChecksCancellation() {
        val fixture = TmdbRetryFixture()
        invokeTmdbFunction("sleepBeforeTmdbRetry", 0L, fixture.call)
        fixture.call.cancel()
        assertThrows(IOException::class.java) {
            invokeTmdbFunction("sleepBeforeTmdbRetry", 0L, fixture.call)
        }
    }

    @Test fun shortDelayIsActuallyWaited() {
        val fixture = TmdbRetryFixture()
        val start = System.nanoTime()
        invokeTmdbFunction("sleepBeforeTmdbRetry", 60L, fixture.call)
        assertTrue(System.nanoTime() - start >= TimeUnit.MILLISECONDS.toNanos(60))
    }

    @Test fun interruptionStopsDelayAndPreservesInterruptFlag() {
        val fixture = TmdbRetryFixture()
        val task = FutureTask<Pair<IOException, Boolean>> {
            val failure = assertThrows(IOException::class.java) {
                invokeTmdbFunction("sleepBeforeTmdbRetry", 30_000L, fixture.call)
            }
            failure to Thread.currentThread().isInterrupted
        }
        val worker = Thread(task, "tmdb-interruption-test")
        worker.start()
        try {
            awaitRetrySleep(worker)
            worker.interrupt()
            val (failure, interrupted) = task.get(2, TimeUnit.SECONDS)
            assertTrue(failure.cause is InterruptedException)
            assertTrue("Interrupt flag must be preserved", interrupted)
        } finally {
            worker.interrupt()
            worker.join(2_000)
            assertFalse("Worker must not leak", worker.isAlive)
        }
    }
}
