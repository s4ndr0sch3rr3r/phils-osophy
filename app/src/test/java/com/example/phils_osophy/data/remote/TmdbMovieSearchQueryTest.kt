package com.example.phils_osophy.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbMovieSearchQueryTest {

    @Test
    fun separatesParenthesizedReleaseYear() {
        val parsed = parseTmdbMovieSearchQuery("The Matrix (1999)")

        assertEquals("The Matrix", parsed.title)
        assertEquals("1999", parsed.releaseYear)
    }

    @Test
    fun keepsOtherTitleParenthesesAndUsesOnlyTrailingYear() {
        val parsed = parseTmdbMovieSearchQuery(
            "Blade Runner (The Final Cut) (1982)"
        )

        assertEquals("Blade Runner (The Final Cut)", parsed.title)
        assertEquals("1982", parsed.releaseYear)
    }

    @Test
    fun acceptsSquareBracketYearSuffix() {
        val parsed = parseTmdbMovieSearchQuery("Heat [1995]")

        assertEquals("Heat", parsed.title)
        assertEquals("1995", parsed.releaseYear)
    }

    @Test
    fun removesUppercaseRegionQualifier() {
        val parsed = parseTmdbMovieSearchQuery("Accused (US)")

        assertEquals("Accused", parsed.title)
        assertNull(parsed.releaseYear)
    }

    @Test
    fun removesLowercaseRegionQualifier() {
        val parsed = parseTmdbMovieSearchQuery("Arriety (uk)")

        assertEquals("Arriety", parsed.title)
        assertNull(parsed.releaseYear)
    }

    @Test
    fun keepsDescriptiveParentheticalSuffixWithoutYear() {
        val parsed = parseTmdbMovieSearchQuery("Blade Runner (The Final Cut)")

        assertEquals("Blade Runner (The Final Cut)", parsed.title)
        assertNull(parsed.releaseYear)
    }

    @Test
    fun leavesTitlesWithoutTrailingQualifierUnchanged() {
        val parsed = parseTmdbMovieSearchQuery("2001: A Space Odyssey")

        assertEquals("2001: A Space Odyssey", parsed.title)
        assertNull(parsed.releaseYear)
    }
}
