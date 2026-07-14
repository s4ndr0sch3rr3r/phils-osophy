from pathlib import Path

PATH = Path("app/src/main/java/com/example/phils_osophy/ui/screens/SeriesDetailScreen.kt")
text = PATH.read_text()


def replace_once(old: str, new: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one match, found {count}: {old[:100]!r}")
    text = text.replace(old, new, 1)


replace_once(
'''    val imageUrl = imagePath?.let { path ->
        "$TMDB_SERIES_BACKDROP_BASE_URL$path"
    }

    Column(modifier = Modifier.fillMaxSize()) {''',
'''    val imageUrl = imagePath?.let { path ->
        "$TMDB_SERIES_BACKDROP_BASE_URL$path"
    }
    val heroMetadataLines = seriesHeroMetadataLines(
        details = details,
        series = series,
        watchedEpisodeCount = effectiveWatchedEpisodes.size,
        completedAtEpochMillis = seriesCompletedAtEpochMillis
    )

    Column(modifier = Modifier.fillMaxSize()) {'''
)

replace_once(
'''        SeriesHero(
            title = title,
            subtitle = seriesSubtitle(details),
            firstAired = seriesFirstAired(details, series),
            tmdbRating = seriesTmdbRating(details, series),
            imageUrl = imageUrl,''',
'''        SeriesHero(
            title = title,
            metadataLines = heroMetadataLines,
            imageUrl = imageUrl,'''
)

replace_once(
'''                SeriesInfoTab(
                    series = series,
                    details = details,
                    completedAtEpochMillis = seriesCompletedAtEpochMillis,
                    errorMessage = errorMessage,''',
'''                SeriesInfoTab(
                    series = series,
                    details = details,
                    errorMessage = errorMessage,'''
)

replace_once(
'''private fun SeriesHero(
    title: String,
    subtitle: String,
    firstAired: String,
    tmdbRating: String,
    imageUrl: String?,''',
'''private fun SeriesHero(
    title: String,
    metadataLines: List<String>,
    imageUrl: String?,'''
)

replace_once('.height(300.dp)', '.height(420.dp)')

replace_once(
'''            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$firstAired • $tmdbRating",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )''',
'''            metadataLines.forEachIndexed { index, line ->
                Spacer(
                    modifier = Modifier.height(
                        if (index == 0) 8.dp else 4.dp
                    )
                )
                Text(
                    text = line,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }'''
)

replace_once(
'''private fun SeriesInfoTab(
    series: SavedSeriesEntity,
    details: SeriesDetailsDto?,
    completedAtEpochMillis: Long?,
    errorMessage: String?,''',
'''private fun SeriesInfoTab(
    series: SavedSeriesEntity,
    details: SeriesDetailsDto?,
    errorMessage: String?,'''
)

replace_once(
'''        item { HorizontalDivider() }

        item {
            CompactSeriesInfo(
                episodeCount = details
                    ?.numberOfEpisodes
                    ?.takeIf { count -> count > 0 }
                    ?.toString()
                    ?: "Unknown",
                addedAt = formatStoredDate(series.addedAtEpochMillis),
                completedAt = completedAtEpochMillis
                    ?.let(::formatStoredDate)
                    ?: "Not completed",
                status = details
                    ?.status
                    ?.takeIf { status -> status.isNotBlank() }
                    ?: seriesStatusLabel(
                        SeriesStatus.fromStorage(series.status)
                    )
            )
        }

        item { HorizontalDivider() }
''',
'''        item { HorizontalDivider() }
'''
)

start = text.index("@Composable\nprivate fun CompactSeriesInfo(")
end = text.index("@Composable\nprivate fun EpisodesTab(", start)
text = text[:start] + text[end:]

old_helpers_start = text.index("private fun seriesSubtitle(")
text = text[:old_helpers_start] + '''private fun seriesHeroMetadataLines(
    details: SeriesDetailsDto?,
    series: SavedSeriesEntity,
    watchedEpisodeCount: Int,
    completedAtEpochMillis: Long?
): List<String> {
    val genres = details
        ?.genres
        .orEmpty()
        .map { genre -> genre.name }
        .filter { name -> name.isNotBlank() }
        .joinToString(", ")
        .ifBlank { "Unknown genre" }
    val seasonCount = details
        ?.numberOfSeasons
        ?.takeIf { count -> count > 0 }
    val seasons = when (seasonCount) {
        1 -> "1 season"
        null -> "Unknown seasons"
        else -> "$seasonCount seasons"
    }
    val totalEpisodeCount = details
        ?.numberOfEpisodes
        ?.takeIf { count -> count > 0 }
    val episodes = when (totalEpisodeCount) {
        1 -> "1 episode"
        null -> "Unknown episodes"
        else -> "$totalEpisodeCount episodes"
    }
    val normalizedWatchedCount = totalEpisodeCount
        ?.let { total -> watchedEpisodeCount.coerceAtMost(total) }
        ?: watchedEpisodeCount
    val watched = totalEpisodeCount
        ?.let { total -> "$normalizedWatchedCount/$total watched" }
        ?: "$normalizedWatchedCount watched"
    val completed = completedAtEpochMillis
        ?.let { timestamp -> "Completed ${formatStoredDate(timestamp)}" }
        ?: "Not completed"
    val status = details
        ?.status
        ?.takeIf { value -> value.isNotBlank() }
        ?: seriesStatusLabel(SeriesStatus.fromStorage(series.status))

    return listOf(
        "$genres • $seasons • $episodes",
        "${seriesTmdbRating(details, series)} • Released ${seriesFirstAired(details, series)}",
        watched,
        "Added ${formatStoredDate(series.addedAtEpochMillis)}",
        "$completed • $status"
    )
}

private fun seriesFirstAired(
    details: SeriesDetailsDto?,
    series: SavedSeriesEntity
): String = details
    ?.firstAirDate
    ?.takeIf { date -> date.isNotBlank() }
    ?: series.firstAirDate.ifBlank { "Unknown release date" }

private fun seriesTmdbRating(
    details: SeriesDetailsDto?,
    series: SavedSeriesEntity
): String = details
    ?.voteAverage
    ?.takeIf { rating -> rating > 0.0 }
    ?.let { rating -> "%.1f / 10".format(rating) }
    ?: if (series.voteAverage > 0.0) {
        "%.1f / 10".format(series.voteAverage)
    } else {
        "Not rated"
    }
'''

PATH.write_text(text)
