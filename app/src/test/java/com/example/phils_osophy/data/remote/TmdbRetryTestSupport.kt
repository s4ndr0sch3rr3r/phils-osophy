package com.example.phils_osophy.data.remote

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

// Exercise the existing private top-level functions without exposing production API.
internal fun invokeTmdbFunction(name: String, vararg arguments: Any?): Any? {
    val method = Class.forName("com.example.phils_osophy.data.remote.TmdbClientKt")
        .declaredMethods.single { it.name == name }
    method.isAccessible = true
    return try {
        method.invoke(null, *arguments)
    } catch (exception: InvocationTargetException) {
        throw exception.targetException
    }
}

internal class TmdbRetryFixture {
    val request: Request = Request.Builder().url("https://example.test/3/search/tv").build()
    // This call is never executed. It supplies OkHttp's real cancellation state only.
    val call: Call = OkHttpClient().newCall(request)
    var attempts = 0

    fun execute(retryCall: Call = call, proceed: (Int) -> Response): Response {
        val chain = Proxy.newProxyInstance(
            Interceptor.Chain::class.java.classLoader,
            arrayOf(Interceptor.Chain::class.java)
        ) { _, method, arguments ->
            when (method.name) {
                "call" -> retryCall
                "proceed" -> {
                    assertEquals(request, arguments!![0])
                    proceed(++attempts)
                }
                else -> error("Unexpected chain method: ${method.name}")
            }
        } as Interceptor.Chain
        return invokeTmdbFunction("executeTmdbRequestWithRetry", chain, request) as Response
    }

    fun response(code: Int, body: TrackingTmdbBody, retryAfter: String? = null): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test response")
            .body(body)
            .apply { retryAfter?.let { header("Retry-After", it) } }
            .build()
}

internal class TrackingTmdbBody : ResponseBody() {
    @Volatile var closed = false
        private set
    private val bufferedSource = object : ForwardingSource(Buffer().writeUtf8("{}")) {
        override fun close() {
            closed = true
            super.close()
        }
    }.buffer()

    override fun contentType(): MediaType? = null
    override fun contentLength(): Long = 2L
    override fun source(): BufferedSource = bufferedSource
}

internal fun awaitRetrySleep(thread: Thread) {
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
    while (thread.isAlive && thread.state != Thread.State.TIMED_WAITING &&
        System.nanoTime() < deadline) {
        Thread.sleep(1)
    }
    assertTrue("Worker must reach the retry sleep", thread.state == Thread.State.TIMED_WAITING)
}
