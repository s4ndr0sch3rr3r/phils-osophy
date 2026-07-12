package com.example.phils_osophy.data.importer

import android.content.Context
import android.net.Uri
import com.example.phils_osophy.data.local.PhilsOsophyDatabase
import com.example.phils_osophy.data.local.SeriesStatus
import com.example.phils_osophy.data.local.WatchedEpisodeEntity
import com.example.phils_osophy.data.local.toSavedMovieEntity
import com.example.phils_osophy.data.local.toSavedSeriesEntity
import com.example.phils_osophy.data.remote.MovieDto
import com.example.phils_osophy.data.remote.SeriesDto
import com.example.phils_osophy.data.remote.TmdbClient
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_GDPR_BACKUP_BYTES = 128 * 1024 * 1024
private const val MAX_GDPR_ENTRY_BYTES = 32 * 1024 * 1024

object TvTimeGdprImporter {

    suspend fun importBackup(
        context: Context,
        uri: Uri
    ): TvTimeImportResult = withContext(Dispatchers.IO) {
        val documents = readCsvDocuments(context, uri)
        val parsed = parseTvTimeDocuments(documents)

        if (parsed.series.isEmpty() && parsed.movies.isEmpty()) {
            throw IllegalArgumentException(
                "No watched TV Time movies or series were found in gdpr-data.zip."
            )
        }

        val database = PhilsOsophyDatabase.getInstance(context)
        val seriesDao = database.savedSeriesDao()
        val movieDao = database.savedMovieDao()
        val watchedEpisodeDao = database.watchedEpisodeDao()

        var importedSeriesCount = 0
        var importedEpisodeCount = 0
        var importedMovieCount = 0
        val skippedTitles = mutableListOf<String>()

        parsed.series.values
            .filter { importedSeries -> importedSeries.episodes.isNotEmpty() }
            .forEach { importedSeries ->
                val tmdbSeries = findSeries(importedSeries.title)
                if (tmdbSeries == null) {
                    skippedTitles += importedSeries.title
                    return@forEach
                }

                val progress = expandProgressToLatestEpisode(
                    seriesId = tmdbSeries.id,
                    explicitEpisodes = importedSeries.episodes
                )
                val status = if (progress.isComplete) {
                    SeriesStatus.FINISHED
                } else {
                    SeriesStatus.IN_PROGRESS
                }
                val earliestWatchedAt = progress.episodes
                    .minOfOrNull { episode -> episode.watchedAtEpochMillis }
                    ?: System.currentTimeMillis()

                val existing = seriesDao.getById(tmdbSeries.id)
                if (existing == null) {
                    seriesDao.insert(
                        tmdbSeries.toSavedSeriesEntity(
                            status = status,
                            addedAtEpochMillis = earliestWatchedAt
                        )
                    )
                } else {
                    seriesDao.updateStatus(
                        seriesId = tmdbSeries.id,
                        status = status.name
                    )
                }

                if (progress.episodes.isNotEmpty()) {
                    watchedEpisodeDao.markWatched(progress.episodes)
                }

                importedSeriesCount += 1
                importedEpisodeCount += progress.episodes.size
            }

        parsed.movies.values.forEach { importedMovie ->
            val tmdbMovie = findMovie(importedMovie.title)
            if (tmdbMovie == null) {
                skippedTitles += importedMovie.title
                return@forEach
            }

            movieDao.insert(
                tmdbMovie.toSavedMovieEntity(
                    addedAtEpochMillis = importedMovie.watchedAtEpochMillis
                        ?: System.currentTimeMillis()
                )
            )
            importedMovieCount += 1
        }

        TvTimeImportResult(
            filesRead = documents.size,
            seriesImported = importedSeriesCount,
            episodesImported = importedEpisodeCount,
            moviesImported = importedMovieCount,
            skippedTitles = skippedTitles.distinct()
        )
    }
}

private data class CsvDocument(
    val name: String,
    val content: String
)

private data class EpisodeKey(
    val seasonNumber: Int,
    val episodeNumber: Int
)

private data class ImportedSeries(
    val title: String,
    val episodes: MutableMap<EpisodeKey, Long?> = linkedMapOf()
)

private data class ImportedMovie(
    val title: String,
    var watchedAtEpochMillis: Long? = null
)

private class ParsedTvTimeBackup {
    val series = linkedMapOf<String, ImportedSeries>()
    val movies = linkedMapOf<String, ImportedMovie>()

    fun addEpisode(
        title: String,
        seasonNumber: Int,
        episodeNumber: Int,
        watchedAtEpochMillis: Long?
    ) {
        if (
            title.isBlank() ||
            seasonNumber <= 0 ||
            episodeNumber <= 0
        ) {
            return
        }

        val cleanTitle = cleanTitle(title)
        val seriesEntry = series.getOrPut(normalizedTitle(cleanTitle)) {
            ImportedSeries(title = cleanTitle)
        }
        val key = EpisodeKey(
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )
        val existingTimestamp = seriesEntry.episodes[key]
        seriesEntry.episodes[key] = when {
            existingTimestamp == null -> watchedAtEpochMillis
            watchedAtEpochMillis == null -> existingTimestamp
            else -> minOf(existingTimestamp, watchedAtEpochMillis)
        }
    }

    fun addMovie(
        title: String,
        watchedAtEpochMillis: Long?
    ) {
        if (title.isBlank()) {
            return
        }

        val cleanTitle = cleanTitle(title)
        val movie = movies.getOrPut(normalizedTitle(cleanTitle)) {
            ImportedMovie(title = cleanTitle)
        }
        movie.watchedAtEpochMillis = when {
            movie.watchedAtEpochMillis == null -> watchedAtEpochMillis
            watchedAtEpochMillis == null -> movie.watchedAtEpochMillis
            else -> minOf(
                movie.watchedAtEpochMillis!!,
                watchedAtEpochMillis
            )
        }
    }
}

private data class ExpandedProgress(
    val episodes: List<WatchedEpisodeEntity>,
    val isComplete: Boolean
)

private fun readCsvDocuments(
    context: Context,
    uri: Uri
): List<CsvDocument> {
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        input.readLimited(MAX_GDPR_BACKUP_BYTES)
    } ?: throw IllegalArgumentException("The selected file could not be opened.")

    val isZip = bytes.size >= 4 &&
        bytes[0] == 0x50.toByte() &&
        bytes[1] == 0x4B.toByte()

    if (!isZip) {
        throw IllegalArgumentException(
            "Select the TV Time file named gdpr-data.zip."
        )
    }

    val documents = mutableListOf<CsvDocument>()
    ZipInputStream(ByteArrayInputStream(bytes)).use { zipInput ->
        var entry = zipInput.nextEntry
        while (entry != null) {
            if (
                !entry.isDirectory &&
                entry.name.lowercase(Locale.ROOT).endsWith(".csv")
            ) {
                val content = zipInput
                    .readLimited(MAX_GDPR_ENTRY_BYTES)
                    .toString(Charsets.UTF_8)
                documents += CsvDocument(
                    name = entry.name.substringAfterLast('/'),
                    content = content
                )
            }
            zipInput.closeEntry()
            entry = zipInput.nextEntry
        }
    }

    if (documents.isEmpty()) {
        throw IllegalArgumentException(
            "gdpr-data.zip does not contain readable TV Time CSV files."
        )
    }

    return documents
}

private fun parseTvTimeDocuments(
    documents: List<CsvDocument>
): ParsedTvTimeBackup {
    val parsed = ParsedTvTimeBackup()

    documents.forEach { document ->
        val rows = parseCsv(document.content)
        rows.forEach { row ->
            val title = firstValue(
                row,
                "tvshowname",
                "seriesname",
                "showname"
            )
            val seasonNumber = firstInt(
                row,
                "episodeseasonnumber",
                "seasonnumber",
                "sno"
            )
            val episodeNumber = firstInt(
                row,
                "episodenumber",
                "epno"
            )

            val watchMarker = firstValue(
                row,
                "gsi",
                "key",
                "typeuuidn"
            )?.lowercase(Locale.ROOT).orEmpty()
            val isWatchedEpisodeRow =
                document.name.equals(
                    "seen_episode.csv",
                    ignoreCase = true
                ) ||
                    document.name.equals(
                        "seen_episode_latest.csv",
                        ignoreCase = true
                    ) ||
                    document.name.equals(
                        "tracking-prod-records-v2.csv",
                        ignoreCase = true
                    ) ||
                    watchMarker.contains("watch-episode") ||
                    watchMarker.contains("rewatch-episode")

            if (
                isWatchedEpisodeRow &&
                !title.isNullOrBlank() &&
                seasonNumber != null &&
                episodeNumber != null
            ) {
                parsed.addEpisode(
                    title = title,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    watchedAtEpochMillis = firstTimestamp(
                        row,
                        "createdat",
                        "updatedat"
                    )
                )
            }

            val entityType = firstValue(
                row,
                "entitytype"
            )?.lowercase(Locale.ROOT)
            val movieAction = firstValue(
                row,
                "type"
            )?.lowercase(Locale.ROOT)
            val movieTitle = firstValue(
                row,
                "moviename",
                "movietitle"
            )
            val isWatchedMovie =
                entityType == "movie" &&
                    movieAction.orEmpty() in setOf("watch", "rewatch_count")

            if (isWatchedMovie && !movieTitle.isNullOrBlank()) {
                parsed.addMovie(
                    title = movieTitle,
                    watchedAtEpochMillis = firstTimestamp(
                        row,
                        "createdat",
                        "updatedat"
                    )
                )
            }
        }
    }

    return parsed
}

private fun parseCsv(content: String): List<Map<String, String>> {
    val lines = content.lineSequence()
        .filter { line -> line.isNotBlank() }
        .toList()
    if (lines.size < 2) {
        return emptyList()
    }

    val headers = parseCsvLine(lines.first())
        .map(::normalizedKey)

    return lines.drop(1).mapNotNull { line ->
        val values = parseCsvLine(line)
        if (values.isEmpty()) {
            return@mapNotNull null
        }

        headers.mapIndexedNotNull { index, header ->
            values.getOrNull(index)
                ?.trim()
                ?.takeIf { value -> value.isNotBlank() }
                ?.let { value -> header to value }
        }.toMap()
    }
}

private fun parseCsvLine(line: String): List<String> {
    val values = mutableListOf<String>()
    val current = StringBuilder()
    var insideQuotes = false
    var index = 0

    while (index < line.length) {
        val character = line[index]
        when {
            character == '"' &&
                insideQuotes &&
                index + 1 < line.length &&
                line[index + 1] == '"' -> {
                current.append('"')
                index += 1
            }

            character == '"' -> {
                insideQuotes = !insideQuotes
            }

            character == ',' && !insideQuotes -> {
                values += current.toString()
                current.clear()
            }

            else -> current.append(character)
        }
        index += 1
    }

    values += current.toString()
    return values
}

private suspend fun expandProgressToLatestEpisode(
    seriesId: Int,
    explicitEpisodes: Map<EpisodeKey, Long?>
): ExpandedProgress {
    val latestKey = explicitEpisodes.keys.maxWithOrNull(
        compareBy<EpisodeKey> { key -> key.seasonNumber }
            .thenBy { key -> key.episodeNumber }
    ) ?: return ExpandedProgress(
        episodes = emptyList(),
        isComplete = false
    )

    val fallbackTimestamp = explicitEpisodes[latestKey]
        ?: explicitEpisodes.values.filterNotNull().maxOrNull()
        ?: System.currentTimeMillis()

    val details = runCatching {
        TmdbClient.api.getSeriesDetails(seriesId)
    }.getOrNull()

    val expandedKeys = explicitEpisodes.keys.toMutableSet()
    details?.seasons
        ?.filter { season ->
            season.seasonNumber > 0 &&
                season.seasonNumber <= latestKey.seasonNumber
        }
        ?.forEach { season ->
            val lastEpisode = when {
                season.seasonNumber < latestKey.seasonNumber ->
                    season.episodeCount
                season.episodeCount > 0 ->
                    minOf(
                        latestKey.episodeNumber,
                        season.episodeCount
                    )
                else -> latestKey.episodeNumber
            }

            if (lastEpisode > 0) {
                (1..lastEpisode).forEach { episodeNumber ->
                    expandedKeys += EpisodeKey(
                        seasonNumber = season.seasonNumber,
                        episodeNumber = episodeNumber
                    )
                }
            }
        }

    val isComplete = details?.seasons
        ?.filter { season ->
            season.seasonNumber > 0 &&
                season.episodeCount > 0
        }
        ?.takeIf { seasons -> seasons.isNotEmpty() }
        ?.all { season ->
            (1..season.episodeCount).all { episodeNumber ->
                EpisodeKey(
                    seasonNumber = season.seasonNumber,
                    episodeNumber = episodeNumber
                ) in expandedKeys
            }
        } ?: false

    val watchedEpisodes = expandedKeys
        .sortedWith(
            compareBy<EpisodeKey> { key -> key.seasonNumber }
                .thenBy { key -> key.episodeNumber }
        )
        .map { key ->
            WatchedEpisodeEntity(
                seriesId = seriesId,
                seasonNumber = key.seasonNumber,
                episodeNumber = key.episodeNumber,
                watchedAtEpochMillis = explicitEpisodes[key]
                    ?: fallbackTimestamp
            )
        }

    return ExpandedProgress(
        episodes = watchedEpisodes,
        isComplete = isComplete
    )
}

private suspend fun findSeries(title: String): SeriesDto? {
    val results = runCatching {
        TmdbClient.api.searchSeries(query = title).results
    }.getOrDefault(emptyList())
    val normalizedRequested = normalizedTitle(title)

    return results.firstOrNull { candidate ->
        normalizedTitle(candidate.name) == normalizedRequested
    } ?: results.firstOrNull()
}

private suspend fun findMovie(title: String): MovieDto? {
    val results = runCatching {
        TmdbClient.api.searchMovies(query = title).results
    }.getOrDefault(emptyList())
    val normalizedRequested = normalizedTitle(title)

    return results.firstOrNull { candidate ->
        normalizedTitle(candidate.title) == normalizedRequested
    } ?: results.firstOrNull()
}

private fun firstValue(
    values: Map<String, String>,
    vararg keys: String
): String? = keys.firstNotNullOfOrNull { key ->
    values[key]?.trim()?.takeIf { value -> value.isNotBlank() }
}

private fun firstInt(
    values: Map<String, String>,
    vararg keys: String
): Int? = firstValue(values, *keys)?.toIntOrNull()

private fun firstTimestamp(
    values: Map<String, String>,
    vararg keys: String
): Long? {
    val rawValue = firstValue(values, *keys) ?: return null

    rawValue.toLongOrNull()?.let { numeric ->
        return if (numeric in 1..9_999_999_999L) {
            numeric * 1000L
        } else {
            numeric
        }
    }

    runCatching {
        return Instant.parse(rawValue).toEpochMilli()
    }

    val localDateTimeFormats = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    )
    localDateTimeFormats.forEach { formatter ->
        runCatching {
            return LocalDateTime.parse(rawValue, formatter)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
    }

    return null
}

private fun InputStream.readLimited(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0

    while (true) {
        val read = read(buffer)
        if (read < 0) {
            break
        }

        total += read
        if (total > maxBytes) {
            throw IllegalArgumentException(
                "The selected gdpr-data.zip file is too large."
            )
        }
        output.write(buffer, 0, read)
    }

    return output.toByteArray()
}

private fun normalizedKey(value: String): String =
    value.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]"), "")

private fun cleanTitle(value: String): String =
    value.trim().replace(Regex("\\s+"), " ")

private fun normalizedTitle(value: String): String {
    val withoutAccents = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")

    return withoutAccents
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]"), "")
}
