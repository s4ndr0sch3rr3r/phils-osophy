package com.example.phils_osophy.data.remote

internal data class TmdbSearchQuery(
    val title: String,
    val year: String?
)

internal data class TmdbMovieSearchQuery(
    val title: String,
    val releaseYear: String?
)

private val TrailingSearchQualifierPattern =
    Regex("""^(.*?)\s*(?:\(([^()]*)\)|\[([^\[\]]*)\])\s*$""")
private val FourDigitYearPattern = Regex("""\d{4}""")
private val RegionQualifierPattern = Regex("""(?i)[a-z]{2,5}""")

internal fun parseTmdbSearchQuery(rawQuery: String): TmdbSearchQuery {
    val trimmedQuery = rawQuery.trim()
    val match = TrailingSearchQualifierPattern.matchEntire(trimmedQuery)
    val title = match
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        .orEmpty()
    val qualifier = match?.let { result ->
        result.groupValues
            .getOrNull(2)
            ?.takeIf { value -> value.isNotBlank() }
            ?: result.groupValues
                .getOrNull(3)
                ?.takeIf { value -> value.isNotBlank() }
    }?.trim()

    if (title.isBlank() || qualifier.isNullOrBlank()) {
        return TmdbSearchQuery(
            title = trimmedQuery,
            year = null
        )
    }

    val year = qualifier.takeIf(FourDigitYearPattern::matches)
    val shouldRemoveQualifier = year != null || RegionQualifierPattern.matches(qualifier)
    return if (shouldRemoveQualifier) {
        TmdbSearchQuery(
            title = title,
            year = year
        )
    } else {
        TmdbSearchQuery(
            title = trimmedQuery,
            year = null
        )
    }
}

internal fun parseTmdbMovieSearchQuery(rawQuery: String): TmdbMovieSearchQuery {
    val parsed = parseTmdbSearchQuery(rawQuery)
    return TmdbMovieSearchQuery(
        title = parsed.title,
        releaseYear = parsed.year
    )
}
