package com.example.phils_osophy.data.remote

import android.util.Log
import com.example.phils_osophy.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val TMDB_HTTP_MAX_RETRIES = 4
private const val TMDB_HTTP_RETRY_BASE_DELAY_MILLIS = 1_000L
private const val TMDB_HTTP_RETRY_MAX_DELAY_MILLIS = 8_000L
private const val TMDB_HTTP_RETRY_AFTER_MAX_DELAY_MILLIS = 30_000L
private const val TMDB_HTTP_LOG_TAG = "TmdbClient"

private val RetryableTmdbStatusCodes = setOf(429, 500, 502, 503, 504)

object TmdbClient {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .callTimeout(2, TimeUnit.MINUTES)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val request = originalRequest
                .newBuilder()
                .url(normalizeMovieSearchUrl(originalRequest.url))
                .header(
                    "Authorization",
                    "Bearer ${BuildConfig.TMDB_READ_TOKEN}"
                )
                .header("Accept", "application/json")
                .build()

            executeTmdbRequestWithRetry(chain, request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .client(httpClient)
        .addConverterFactory(
            json.asConverterFactory(
                "application/json".toMediaType()
            )
        )
        .build()

    val api: TmdbApi = retrofit.create(TmdbApi::class.java)
}

private fun normalizeMovieSearchUrl(url: HttpUrl): HttpUrl {
    if (!url.encodedPath.endsWith("/search/movie")) {
        return url
    }

    val rawQuery = url.queryParameter("query") ?: return url
    val parsedQuery = parseTmdbMovieSearchQuery(rawQuery)
    val releaseYear = parsedQuery.releaseYear ?: return url

    return url.newBuilder()
        .setQueryParameter("query", parsedQuery.title)
        .setQueryParameter("primary_release_year", releaseYear)
        .build()
}

private fun executeTmdbRequestWithRetry(
    chain: Interceptor.Chain,
    request: Request
): Response {
    var retryCount = 0

    while (true) {
        try {
            val response = chain.proceed(request)
            if (
                response.code !in RetryableTmdbStatusCodes ||
                retryCount >= TMDB_HTTP_MAX_RETRIES
            ) {
                return response
            }

            val delayMillis = tmdbRetryDelayMillis(
                retryCount = retryCount,
                retryAfterHeader = response.header("Retry-After")
            )
            Log.w(
                TMDB_HTTP_LOG_TAG,
                "TMDB returned HTTP ${response.code} for ${request.url.encodedPath}; " +
                    "retry ${retryCount + 1}/$TMDB_HTTP_MAX_RETRIES in ${delayMillis}ms"
            )
            response.close()
            sleepBeforeTmdbRetry(delayMillis)
        } catch (exception: IOException) {
            if (retryCount >= TMDB_HTTP_MAX_RETRIES) {
                throw exception
            }

            val delayMillis = tmdbRetryDelayMillis(
                retryCount = retryCount,
                retryAfterHeader = null
            )
            Log.w(
                TMDB_HTTP_LOG_TAG,
                "TMDB request failed for ${request.url.encodedPath}; " +
                    "retry ${retryCount + 1}/$TMDB_HTTP_MAX_RETRIES in ${delayMillis}ms",
                exception
            )
            sleepBeforeTmdbRetry(delayMillis)
        }

        retryCount += 1
    }
}

private fun tmdbRetryDelayMillis(
    retryCount: Int,
    retryAfterHeader: String?
): Long {
    val retryAfterMillis = retryAfterHeader
        ?.trim()
        ?.toLongOrNull()
        ?.times(1_000L)
        ?.coerceIn(0L, TMDB_HTTP_RETRY_AFTER_MAX_DELAY_MILLIS)
    if (retryAfterMillis != null) {
        return retryAfterMillis
    }

    return (TMDB_HTTP_RETRY_BASE_DELAY_MILLIS * (1L shl retryCount))
        .coerceAtMost(TMDB_HTTP_RETRY_MAX_DELAY_MILLIS)
}

private fun sleepBeforeTmdbRetry(delayMillis: Long) {
    try {
        Thread.sleep(delayMillis)
    } catch (exception: InterruptedException) {
        Thread.currentThread().interrupt()
        throw IOException("TMDB retry was interrupted.", exception)
    }
}
