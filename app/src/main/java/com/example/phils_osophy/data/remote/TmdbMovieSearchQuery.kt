package com.example.phils_osophy.data.remote

internal data class TmdbMovieSearchQuery(
    val title: String,
    val releaseYear: String?
)

private val TrailingReleaseYearPattern =
    Regex("""^(.*?)\s*(?:\((\d{4})\)|\[(\d{4})\])\s*$""")

internal fun parseTmdbMovieSearchQuery(rawQuery: String): TmdbMovieSearchQuery {
    val trimmedQuery = rawQuery.trim()
    val match = TrailingReleaseYearPattern.matchEntire(trimmedQuery)
    val title = match
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        .orEmpty()
    val releaseYear = match?.let { result ->
        result.groupValues
            .getOrNull(2)
            ?.takeIf { value -> value.isNotBlank() }
            ?: result.groupValues
                .getOrNull(3)
                ?.takeIf { value -> value.isNotBlank() }
    }

    return if (title.isNotBlank() && releaseYear != null) {
        TmdbMovieSearchQuery(
            title = title,
            releaseYear = releaseYear
        )
    } else {
        TmdbMovieSearchQuery(
            title = trimmedQuery,
            releaseYear = null
        )
    }
}
