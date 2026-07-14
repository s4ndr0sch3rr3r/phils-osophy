package com.example.phils_osophy.data.importer

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.phils_osophy.data.local.PhilsOsophyDatabase
import com.example.phils_osophy.data.local.SeriesStatus
import com.example.phils_osophy.data.local.WatchedEpisodeEntity
import com.example.phils_osophy.data.local.toSavedMovieEntity
import com.example.phils_osophy.data.local.toSavedSeriesEntity
import com.example.phils_osophy.data.remote.MovieDto
import com.example.phils_osophy.data.remote.SeriesDto
import com.example.phils_osophy.data.remote.TmdbClient
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private const val PARALLEL_SERIES_HISTORY_FILE = "tracking-prod-records-v2.csv"
private const val PARALLEL_MOVIE_HISTORY_FILE = "tracking-prod-records.csv"
private const val PARALLEL_MAX_ARCHIVE_BYTES = 128L * 1024L * 1024L
private const val PARALLEL_MAX_ENTRY_BYTES = 32 * 1024 * 1024
private const val MAX_CONCURRENT_TMDB_LOOKUPS = 4
private const val IMPORT_LOG_TAG = "TvTimeGdprImport"

private val ParallelRequiredHistoryFiles = setOf(
    PARALLEL_SERIES_HISTORY_FILE,
    PARALLEL_MOVIE_HISTORY_FILE
)

private val ParallelWatchedMovieActions = setOf("watch", "rewatch_count")

object ParallelTvTimeGdprImporter {

    suspend fun importBackup(
        context: Context,
        uri: Uri
    ): TvTimeImportResult = withContext(Dispatchers.IO) {
        val files = readHistoryFiles(context, uri)
        val parsed = parseHistory(files)
        if (parsed.series.isEmpty() && parsed.movies.isEmpty()) {
            throw IllegalArgumentException(
                "No watched TV Time movies or series were found in gdpr-data.zip."
            )
        }

        val seriesEntries = parsed.series.values.toList()
        val movieEntries = parsed.movies.values.toList()
        Log.i(
            IMPORT_LOG_TAG,
            "Starting TMDB matching for ${seriesEntries.size} series and " +
                "${movieEntries.size} movies"
        )

        val semaphore = Semaphore(MAX_CONCURRENT_TMDB_LOOKUPS)
        val resolved = coroutineScope {
            val seriesDeferred = seriesEntries.mapIndexed { index, imported ->
                async {
                    semaphore.withPermit {
                        resolveSeries(imported, index, seriesEntries.size)
                    }
                }
            }
            val movieDeferred = movieEntries.mapIndexed { index, imported ->
                async {
                    semaphore.withPermit {
                        resolveMovie(imported, index, movieEntries.size)
                    }
                }
            }
            ResolvedImport(
                series = seriesDeferred.awaitAll(),
                movies = movieDeferred.awaitAll()
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

        resolved.series.forEach { item ->
            val tmdb = item.tmdb
            val progress = item.progress
            if (tmdb == null || progress == null) {
                skippedTitles += item.imported.title
                return@forEach
            }
            val status = if (progress.isComplete) {
                SeriesStatus.FINISHED
            } else {
                SeriesStatus.IN_PROGRESS
            }
            val earliestWatchedAt = progress.episodes
                .minOfOrNull { episode -> episode.watchedAtEpochMillis }
                ?: System.currentTimeMillis()
            val existing = seriesDao.getById(tmdb.id)
            if (existing == null) {
                seriesDao.insert(
                    tmdb.toSavedSeriesEntity(
                        status = status,
                        addedAtEpochMillis = earliestWatchedAt
                    )
                )
            } else {
                seriesDao.updateStatus(seriesId = tmdb.id, status = status.name)
            }
            if (progress.episodes.isNotEmpty()) {
                watchedEpisodeDao.markWatched(progress.episodes)
            }
            importedSeriesCount += 1
            importedEpisodeCount += progress.episodes.size
        }

        resolved.movies.forEach { item ->
            val tmdb = item.tmdb
            if (tmdb == null) {
                skippedTitles += item.imported.title
                return@forEach
            }
            movieDao.insert(
                tmdb.toSavedMovieEntity(
                    addedAtEpochMillis = item.imported.watchedAtEpochMillis
                        ?: System.currentTimeMillis()
                )
            )
            importedMovieCount += 1
        }

        val distinctSkippedTitles = skippedTitles.distinct()
        Log.i(
            IMPORT_LOG_TAG,
            "Import complete: $importedSeriesCount series, $importedMovieCount movies, " +
                "${distinctSkippedTitles.size} unmatched titles"
        )
        TvTimeImportResult(
            filesRead = files.size,
            seriesImported = importedSeriesCount,
            episodesImported = importedEpisodeCount,
            moviesImported = importedMovieCount,
            skippedTitles = distinctSkippedTitles
        )
    }
}

private data class ParallelEpisodeKey(
    val seasonNumber: Int,
    val episodeNumber: Int
)

private data class ParallelImportedSeries(
    val title: String,
    val episodes: MutableMap<ParallelEpisodeKey, Long?> = linkedMapOf()
)

private data class ParallelImportedMovie(
    val title: String,
    var watchedAtEpochMillis: Long? = null
)

private class ParallelParsedHistory {
    val series = linkedMapOf<String, ParallelImportedSeries>()
    val movies = linkedMapOf<String, ParallelImportedMovie>()

    fun addEpisode(title: String, season: Int, episode: Int, watchedAt: Long?) {
        if (title.isBlank() || season <= 0 || episode <= 0) return
        val clean = cleanParallelTitle(title)
        val imported = series.getOrPut(normalizedParallelTitle(clean)) {
            ParallelImportedSeries(clean)
        }
        val key = ParallelEpisodeKey(season, episode)
        val existing = imported.episodes[key]
        imported.episodes[key] = when {
            existing == null -> watchedAt
            watchedAt == null -> existing
            else -> minOf(existing, watchedAt)
        }
    }

    fun addMovie(title: String, watchedAt: Long?) {
        if (title.isBlank()) return
        val clean = cleanParallelTitle(title)
        val imported = movies.getOrPut(normalizedParallelTitle(clean)) {
            ParallelImportedMovie(clean)
        }
        imported.watchedAtEpochMillis = when {
            imported.watchedAtEpochMillis == null -> watchedAt
            watchedAt == null -> imported.watchedAtEpochMillis
            else -> minOf(imported.watchedAtEpochMillis!!, watchedAt)
        }
    }
}

private data class ParallelExpandedProgress(
    val episodes: List<WatchedEpisodeEntity>,
    val isComplete: Boolean
)

private data class ResolvedParallelSeries(
    val imported: ParallelImportedSeries,
    val tmdb: SeriesDto?,
    val progress: ParallelExpandedProgress?
)

private data class ResolvedParallelMovie(
    val imported: ParallelImportedMovie,
    val tmdb: MovieDto?
)

private data class ResolvedImport(
    val series: List<ResolvedParallelSeries>,
    val movies: List<ResolvedParallelMovie>
)

private suspend fun resolveSeries(
    imported: ParallelImportedSeries,
    index: Int,
    total: Int
): ResolvedParallelSeries {
    Log.d(IMPORT_LOG_TAG, "Loading series ${index + 1}/$total: ${imported.title}")
    val tmdb = findParallelSeries(imported.title)
    if (tmdb == null) {
        Log.w(IMPORT_LOG_TAG, "Series not found: ${imported.title}")
        return ResolvedParallelSeries(imported, null, null)
    }
    Log.d(
        IMPORT_LOG_TAG,
        "Matched series ${imported.title} to ${tmdb.name} (${tmdb.id})"
    )
    return ResolvedParallelSeries(
        imported = imported,
        tmdb = tmdb,
        progress = expandParallelProgress(tmdb.id, imported.episodes)
    )
}

private suspend fun resolveMovie(
    imported: ParallelImportedMovie,
    index: Int,
    total: Int
): ResolvedParallelMovie {
    Log.d(IMPORT_LOG_TAG, "Loading movie ${index + 1}/$total: ${imported.title}")
    val tmdb = findParallelMovie(imported.title)
    if (tmdb == null) {
        Log.w(IMPORT_LOG_TAG, "Movie not found: ${imported.title}")
    } else {
        Log.d(
            IMPORT_LOG_TAG,
            "Matched movie ${imported.title} to ${tmdb.title} (${tmdb.id})"
        )
    }
    return ResolvedParallelMovie(imported, tmdb)
}

private suspend fun findParallelSeries(title: String): SeriesDto? {
    val results = try {
        TmdbClient.api.searchSeries(query = title).results
    } catch (exception: Exception) {
        if (exception is CancellationException) throw exception
        Log.e(IMPORT_LOG_TAG, "Series lookup failed: $title", exception)
        emptyList()
    }
    val requested = normalizedParallelTitle(title)
    return results.firstOrNull { normalizedParallelTitle(it.name) == requested }
        ?: results.firstOrNull()
}

private suspend fun findParallelMovie(title: String): MovieDto? {
    val results = try {
        TmdbClient.api.searchMovies(query = title).results
    } catch (exception: Exception) {
        if (exception is CancellationException) throw exception
        Log.e(IMPORT_LOG_TAG, "Movie lookup failed: $title", exception)
        emptyList()
    }
    val requested = normalizedParallelTitle(title)
    return results.firstOrNull { normalizedParallelTitle(it.title) == requested }
        ?: results.firstOrNull()
}

private suspend fun expandParallelProgress(
    seriesId: Int,
    explicitEpisodes: Map<ParallelEpisodeKey, Long?>
): ParallelExpandedProgress {
    val latestKey = explicitEpisodes.keys.maxWithOrNull(
        compareBy<ParallelEpisodeKey> { it.seasonNumber }.thenBy { it.episodeNumber }
    ) ?: return ParallelExpandedProgress(emptyList(), false)
    val fallbackTimestamp = explicitEpisodes[latestKey]
        ?: explicitEpisodes.values.filterNotNull().maxOrNull()
        ?: System.currentTimeMillis()
    val details = try {
        TmdbClient.api.getSeriesDetails(seriesId)
    } catch (exception: Exception) {
        if (exception is CancellationException) throw exception
        Log.e(
            IMPORT_LOG_TAG,
            "Could not load series details for TMDB id $seriesId",
            exception
        )
        null
    }
    val expandedKeys = explicitEpisodes.keys.toMutableSet()
    details?.seasons
        ?.filter { it.seasonNumber > 0 && it.seasonNumber <= latestKey.seasonNumber }
        ?.forEach { season ->
            val lastEpisode = when {
                season.seasonNumber < latestKey.seasonNumber -> season.episodeCount
                season.episodeCount > 0 -> minOf(latestKey.episodeNumber, season.episodeCount)
                else -> latestKey.episodeNumber
            }
            if (lastEpisode > 0) {
                (1..lastEpisode).forEach { episode ->
                    expandedKeys += ParallelEpisodeKey(season.seasonNumber, episode)
                }
            }
        }
    val knownSeasons = details?.seasons
        ?.filter { it.seasonNumber > 0 && it.episodeCount > 0 }
        .orEmpty()
    val isComplete = knownSeasons.isNotEmpty() && knownSeasons.all { season ->
        (1..season.episodeCount).all { episode ->
            ParallelEpisodeKey(season.seasonNumber, episode) in expandedKeys
        }
    }
    val watched = expandedKeys
        .sortedWith(compareBy<ParallelEpisodeKey> { it.seasonNumber }.thenBy { it.episodeNumber })
        .map { key ->
            WatchedEpisodeEntity(
                seriesId = seriesId,
                seasonNumber = key.seasonNumber,
                episodeNumber = key.episodeNumber,
                watchedAtEpochMillis = explicitEpisodes[key] ?: fallbackTimestamp
            )
        }
    return ParallelExpandedProgress(watched, isComplete)
}

private fun readHistoryFiles(context: Context, uri: Uri): Map<String, String> {
    val archive = File.createTempFile("tv-time-gdpr-", ".zip", context.cacheDir)
    try {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("The selected file could not be opened.")
        input.use { source ->
            archive.outputStream().use { destination ->
                source.copyParallelLimitedTo(destination, PARALLEL_MAX_ARCHIVE_BYTES)
            }
        }
        return ZipFile(archive).use { zip ->
            val entries = zip.entries().asSequence()
                .filterNot { it.isDirectory }
                .associateBy {
                    it.name.substringAfterLast('/').lowercase(Locale.ROOT)
                }
            val missing = ParallelRequiredHistoryFiles - entries.keys
            if (missing.isNotEmpty()) {
                throw IllegalArgumentException(
                    "gdpr-data.zip is missing: ${missing.sorted().joinToString()}."
                )
            }
            ParallelRequiredHistoryFiles.associateWith { name ->
                zip.getInputStream(entries.getValue(name)).use { entry ->
                    entry.readParallelLimited(PARALLEL_MAX_ENTRY_BYTES).toString(Charsets.UTF_8)
                }
            }
        }
    } catch (_: ZipException) {
        throw IllegalArgumentException("Select the TV Time file named gdpr-data.zip.")
    } finally {
        archive.delete()
    }
}

private fun parseHistory(files: Map<String, String>): ParallelParsedHistory {
    val parsed = ParallelParsedHistory()
    parseParallelCsv(files.getValue(PARALLEL_SERIES_HISTORY_FILE)).forEach { row ->
        val marker = listOfNotNull(row["gsi"], row["key"])
            .joinToString(" ").lowercase(Locale.ROOT)
        if (!marker.contains("watch-episode") && !marker.contains("rewatch-episode")) {
            return@forEach
        }
        val title = firstParallelValue(row, "seriesname") ?: return@forEach
        val season = firstParallelInt(row, "seasonnumber", "sno") ?: return@forEach
        val episode = firstParallelInt(row, "episodenumber", "epno") ?: return@forEach
        parsed.addEpisode(
            title,
            season,
            episode,
            firstParallelTimestamp(row, "createdat", "updatedat")
        )
    }
    parseParallelCsv(files.getValue(PARALLEL_MOVIE_HISTORY_FILE)).forEach { row ->
        val entityType = firstParallelValue(row, "entitytype")?.lowercase(Locale.ROOT)
        val action = firstParallelValue(row, "type")?.lowercase(Locale.ROOT).orEmpty()
        if (entityType != "movie" || action !in ParallelWatchedMovieActions) return@forEach
        val title = firstParallelValue(row, "moviename") ?: return@forEach
        parsed.addMovie(title, firstParallelTimestamp(row, "createdat", "updatedat"))
    }
    return parsed
}

private fun parseParallelCsv(content: String): List<Map<String, String>> {
    val lines = content.lineSequence().filter { it.isNotBlank() }.toList()
    if (lines.size < 2) return emptyList()
    val headers = parseParallelCsvLine(lines.first()).map(::normalizedParallelKey)
    return lines.drop(1).mapNotNull { line ->
        val values = parseParallelCsvLine(line)
        if (values.isEmpty()) return@mapNotNull null
        headers.mapIndexedNotNull { index, header ->
            values.getOrNull(index)?.trim()?.takeIf { it.isNotBlank() }?.let { header to it }
        }.toMap()
    }
}

private fun parseParallelCsvLine(line: String): List<String> {
    val values = mutableListOf<String>()
    val current = StringBuilder()
    var quoted = false
    var index = 0
    while (index < line.length) {
        val character = line[index]
        when {
            character == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                current.append('"')
                index += 1
            }
            character == '"' -> quoted = !quoted
            character == ',' && !quoted -> {
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

private fun firstParallelValue(values: Map<String, String>, vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key ->
        values[key]?.trim()?.takeIf { it.isNotBlank() }
    }

private fun firstParallelInt(values: Map<String, String>, vararg keys: String): Int? =
    firstParallelValue(values, *keys)?.toIntOrNull()

private fun firstParallelTimestamp(values: Map<String, String>, vararg keys: String): Long? {
    val raw = firstParallelValue(values, *keys) ?: return null
    raw.toLongOrNull()?.let { numeric ->
        return if (numeric in 1..9_999_999_999L) numeric * 1000L else numeric
    }
    runCatching { return Instant.parse(raw).toEpochMilli() }
    listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    ).forEach { formatter ->
        runCatching {
            return LocalDateTime.parse(raw, formatter)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
    }
    return null
}

private fun InputStream.copyParallelLimitedTo(destination: OutputStream, maxBytes: Long) {
    val buffer = ByteArray(8 * 1024)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) {
            throw IllegalArgumentException("The selected gdpr-data.zip file is too large.")
        }
        destination.write(buffer, 0, read)
    }
}

private fun InputStream.readParallelLimited(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) {
            throw IllegalArgumentException("A required TV Time history file is too large.")
        }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private fun normalizedParallelKey(value: String): String =
    value.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")

private fun cleanParallelTitle(value: String): String =
    value.trim().replace(Regex("\\s+"), " ")

private fun normalizedParallelTitle(value: String): String {
    val withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
    return withoutAccents.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
}
