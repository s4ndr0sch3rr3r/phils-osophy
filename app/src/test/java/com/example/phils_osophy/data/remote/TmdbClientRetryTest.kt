package com.example.phils_osophy.data.remote

import java.io.IOException
import java.lang.reflect.Proxy
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import okhttp3.Call
import org.junit.Assert.*
import org.junit.Test

class TmdbClientRetryTest {
    @Test fun canceledAtNextAttemptBoundaryDoesNotProceedAgain() {
        val fixture = TmdbRetryFixture()
        var cancellationChecks = 0
        // Active before/after the first response and after the zero delay, canceled
        // at the start of the next loop. This isolates the pre-retry guard.
        val call = Proxy.newProxyInstance(
            Call::class.java.classLoader, arrayOf(Call::class.java)
        ) { _, method, _ ->
            check(method.name == "isCanceled")
            ++cancellationChecks >= 4
        } as Call
        val body = TrackingTmdbBody()
        try {
            assertThrows(IOException::class.java) {
                fixture.execute(call) { fixture.response(503, body, "0") }
            }
            assertEquals(4, cancellationChecks)
            assertEquals(1, fixture.attempts)
            assertTrue(body.closed)
        } finally {
            body.close()
        }
    }

    @Test fun canceledBeforeFirstAttemptDoesNotProceed() {
        val fixture = TmdbRetryFixture()
        fixture.call.cancel()
        assertThrows(IOException::class.java) { fixture.execute { error("Must not proceed") } }
        assertEquals(0, fixture.attempts)
    }

    @Test fun cancellationIOExceptionIsPropagatedWithoutRetry() {
        val fixture = TmdbRetryFixture()
        val failure = IOException("Canceled transport")
        val thrown = assertThrows(IOException::class.java) {
            fixture.execute {
                fixture.call.cancel()
                throw failure
            }
        }
        assertSame(failure, thrown)
        assertEquals(1, fixture.attempts)
    }

    @Test fun cancellationAfterResponseClosesBodyAndDoesNotRetry() {
        // A success, a non-retryable failure and a retryable failure must all be discarded.
        for (code in listOf(200, 404, 503)) {
            val fixture = TmdbRetryFixture()
            val body = TrackingTmdbBody()
            try {
                assertThrows(IOException::class.java) {
                    fixture.execute {
                        fixture.call.cancel()
                        fixture.response(code, body, "0")
                    }
                }
                assertTrue("Canceled HTTP $code body must be closed", body.closed)
                assertEquals(1, fixture.attempts)
            } finally {
                body.close()
            }
        }
    }

    @Test fun cancellationDuringLongRetryDelayStopsAndReleasesBody() {
        val fixture = TmdbRetryFixture()
        val body = TrackingTmdbBody()
        val task = FutureTask<IOException> {
            assertThrows(IOException::class.java) {
                fixture.execute { fixture.response(429, body, "30") }
            }
        }
        val worker = Thread(task, "tmdb-cancellation-test")
        worker.start()
        try {
            awaitRetrySleep(worker)
            assertTrue("Body must be released before waiting", body.closed)
            fixture.call.cancel()
            // Generous scheduling tolerance, but far below the 30-second Retry-After.
            task.get(2, TimeUnit.SECONDS)
            assertEquals(1, fixture.attempts)
        } finally {
            fixture.call.cancel()
            worker.interrupt()
            worker.join(2_000)
            body.close()
            assertFalse("Worker must not leak", worker.isAlive)
        }
    }

    @Test fun everyRetryableStatusCanRecoverAndClosesPreviousResponse() {
        for (code in listOf(429, 500, 502, 503, 504)) {
            val fixture = TmdbRetryFixture()
            val failedBody = TrackingTmdbBody()
            val successBody = TrackingTmdbBody()
            try {
                fixture.execute { attempt ->
                    if (attempt == 1) fixture.response(code, failedBody, "0")
                    else {
                        assertTrue(failedBody.closed)
                        fixture.response(200, successBody)
                    }
                }.use { response ->
                    assertEquals(200, response.code)
                    assertFalse("Returned body remains caller-owned", successBody.closed)
                }
                assertEquals(2, fixture.attempts)
                assertTrue(successBody.closed)
            } finally {
                failedBody.close()
                successBody.close()
            }
        }
    }

    @Test fun repeatedHttpFailuresStopAfterFourRetries() {
        val fixture = TmdbRetryFixture()
        val bodies = mutableListOf<TrackingTmdbBody>()
        try {
            fixture.execute {
                assertTrue(bodies.all { it.closed })
                val body = TrackingTmdbBody().also { bodies.add(it) }
                fixture.response(503, body, "0")
            }.use { response ->
                assertEquals(503, response.code)
                assertEquals(5, fixture.attempts)
                assertTrue(bodies.take(4).all { it.closed })
                assertFalse(bodies.last().closed)
            }
            assertTrue(bodies.all { it.closed })
        } finally {
            bodies.forEach { it.close() }
        }
    }

    @Test fun successfulAndNonRetryableResponsesAreReturnedImmediately() {
        for (code in listOf(200, 400, 401, 404, 501)) {
            val fixture = TmdbRetryFixture()
            val body = TrackingTmdbBody()
            try {
                fixture.execute { fixture.response(code, body) }.use { response ->
                    assertEquals(code, response.code)
                    assertEquals(1, fixture.attempts)
                    assertFalse(body.closed)
                }
            } finally {
                body.close()
            }
        }
    }

    @Test(timeout = 5_000) fun genuineIOExceptionCanRecover() {
        val fixture = TmdbRetryFixture()
        val body = TrackingTmdbBody()
        try {
            val start = System.nanoTime()
            fixture.execute { attempt ->
                if (attempt == 1) throw IOException("Transient failure")
                fixture.response(200, body)
            }.use { assertEquals(200, it.code) }
            assertEquals(2, fixture.attempts)
            assertTrue(System.nanoTime() - start >= TimeUnit.SECONDS.toNanos(1))
        } finally {
            body.close()
        }
    }

    // Uses the real 1 + 2 + 4 + 8 second backoff; no network is involved.
    @Test(timeout = 25_000) fun genuineIOExceptionStopsAfterFourRetries() {
        val fixture = TmdbRetryFixture()
        val failure = IOException("Persistent failure")
        val thrown = assertThrows(IOException::class.java) {
            fixture.execute { throw failure }
        }
        assertSame(failure, thrown)
        assertEquals(5, fixture.attempts)
    }
}
