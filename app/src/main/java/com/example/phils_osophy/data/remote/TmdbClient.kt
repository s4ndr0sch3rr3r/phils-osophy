package com.example.phils_osophy.data.remote

import com.example.phils_osophy.BuildConfig
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

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

            chain.proceed(request)
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
