package com.example.phils_osophy.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbSearchUrlTest {

    @Test
    fun stripsSeriesYearAndAppliesFirstAirDateFilter() {
        val url = "https://api.themoviedb.org/3/search/tv".toHttpUrl()
            .newBuilder()
            .addQueryParameter("query", "Avatar: The Last Airbender (2024)")
            .build()

        val normalized = normalizeTmdbSearchUrl(url)

        assertEquals("Avatar: The Last Airbender", normalized.queryParameter("query"))
        assertEquals("2024", normalized.queryParameter("first_air_date_year"))
        assertNull(normalized.queryParameter("primary_release_year"))
    }

    @Test
    fun stripsMovieRegionWithoutAddingYearFilter() {
        val url = "https://api.themoviedb.org/3/search/movie".toHttpUrl()
            .newBuilder()
            .addQueryParameter("query", "Arriety (UK)")
            .build()

        val normalized = normalizeTmdbSearchUrl(url)

        assertEquals("Arriety", normalized.queryParameter("query"))
        assertNull(normalized.queryParameter("primary_release_year"))
    }

    @Test
    fun appliesMovieReleaseYearFilter() {
        val url = "https://api.themoviedb.org/3/search/movie".toHttpUrl()
            .newBuilder()
            .addQueryParameter("query", "Before (2024)")
            .build()

        val normalized = normalizeTmdbSearchUrl(url)

        assertEquals("Before", normalized.queryParameter("query"))
        assertEquals("2024", normalized.queryParameter("primary_release_year"))
    }
}
