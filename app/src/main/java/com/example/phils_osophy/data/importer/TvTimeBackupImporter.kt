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
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

private const val MAX_BACKUP_BYTES = 64 * 1024 * 1024
private const val MAX_TEXT_ENTRY_BYTES = 12 * 1024 * 1024

private val backupJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

data class TvTimeImportResult(
    val filesRead: Int,
    val seriesImported: Int,
    val episodesImported: Int,
    val moviesImported: Int,
    val skippedTitles: List<String>
) {
    fun summary(): String {
        val imported = buildList {
            add("$seriesImported series")
            add("$episodesImported watched episodes")
            add("$moviesImported movies")
        }.joinToString(", ")

        return if (skippedTitles.isEmpty()) {
            "Imported $imported from $filesRead backup files."
        } else {
            "Imported $imported. Could not match ${skippedTitles.size} titles: " +
                skippedTitles.take(6).joinToString(", ") +
                if (skippedTitles.size > 6) "…" else ""
        }
    }
}

object TvTimeBackupImporter {

    suspend fun importBackup(
        context: Context,
        uri: Uri
    ): TvTimeImportResult = withContext(Dispatchers.IO) {
        val documents = readBackupDocuments(context, uri)
        val parsedBackup = parseDocuments(documents)

        if (parsedBackup.series.isEmpty() && parsedBackup.movies.isEmpty()) {
            throw IllegalArgumentException(
                "No TV Time shows, watched episodes, or movies were found in this file."
            )
        }

        val database = PhilsOsophyDatabase.getInstance(context)
        val seriesDao = database.savedSeriesDao()
        val movieDao = database.savedMovieDao()
        val watchedEpisodeDao = database.watchedEpisodeDao()

        var seriesImported = 0
        var episodesImported = 0
        var moviesImported = 0
        val skippedTitles = mutableListOf<String>()

        parsedBackup.series.values.forEach { importedSeries ->
            val tmdbSeries = findSeries(importedSeries.title)
            if (tmdbSeries == null) {
                skippedTitles += importedSeries.title
                return@forEach
            }

            val status = when {
                importedSeries.stopped -> SeriesStatus.STOPPED
                importedSeries.completed -> SeriesStatus.FINISHED
                importedSeries.episodes.isNotEmpty() -> SeriesStatus.IN_PROGRESS
                else -> SeriesStatus.TO_WATCH
            }
            val earliestActivity = importedSeries.episodes.values
                .filterNotNull()
                .minOrNull()
                ?: System.currentTimeMillis()

            val existing = seriesDao.getById(tmdbSeries.id)
            if (existing == null) {
                seriesDao.insert(
                    tmdbSeries.toSavedSeriesEntity(
                        status = status,
                        addedAtEpochMillis = earliestActivity
                    )
                )
            } else {
                seriesDao.updateStatus(
                    seriesId = tmdbSeries.id,
                    status = status.name
                )
            }

            val watchedEpisodes = importedSeries.episodes.map { (key, watchedAt) ->
                WatchedEpisodeEntity(
                    seriesId = tmdbSeries.id,
                    seasonNumber = key.seasonNumber,
                    episodeNumber = key.episodeNumber,
                    watchedAtEpochMillis = watchedAt
                        ?: System.currentTimeMillis()
                )
            }

            if (watchedEpisodes.isNotEmpty()) {
                watchedEpisodeDao.markWatched(watchedEpisodes)
            }

            seriesImported += 1
            episodesImported += watchedEpisodes.size
        }

        parsedBackup.movies.values.forEach { importedMovie ->
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
            moviesImported += 1
        }

        TvTimeImportResult(
            filesRead = documents.size,
            seriesImported = seriesImported,
            episodesImported = episodesImported,
            moviesImported = moviesImported,
            skippedTitles = skippedTitles.distinct()
        )
    }
}

private data class BackupDocument(
    val name: String,
    val content: String
)

private data class EpisodeKey(
    val seasonNumber: Int,
    val episodeNumber: Int
)

private data class ImportedSeries(
    val title: String,
    var stopped: Boolean = false,
    var completed: Boolean = false,
    val episodes: MutableMap<EpisodeKey, Long?> = linkedMapOf()
)

private data class ImportedMovie(
    val title: String,
    var watchedAtEpochMillis: Long? = null
)

private class ParsedBackup {
    val series = linkedMapOf<String, ImportedSeries>()
    val movies = linkedMapOf<String, ImportedMovie>()

    fun addSeries(
        title: String,
        stopped: Boolean = false,
        completed: Boolean = false
    ): ImportedSeries {
        val cleanTitle = cleanTitle(title)
        val key = normalizedMatchValue(cleanTitle)
        val seriesEntry = series.getOrPut(key) {
            ImportedSeries(title = cleanTitle)
        }
        seriesEntry.stopped = seriesEntry.stopped || stopped
        seriesEntry.completed = seriesEntry.completed || completed
        return seriesEntry
    }

    fun addEpisode(
        showTitle: String,
        seasonNumber: Int,
        episodeNumber: Int,
        watchedAtEpochMillis: Long?
    ) {
        if (seasonNumber <= 0 || episodeNumber <= 0) {
            return
        }
        addSeries(showTitle).episodes[
            EpisodeKey(seasonNumber, episodeNumber)
        ] = watchedAtEpochMillis
    }

    fun addMovie(
        title: String,
        watchedAtEpochMillis: Long?
    ) {
        val cleanTitle = cleanTitle(title)
        val key = normalizedMatchValue(cleanTitle)
        val movie = movies.getOrPut(key) {
            ImportedMovie(cleanTitle)
        }
        if (movie.watchedAtEpochMillis == null) {
            movie.watchedAtEpochMillis = watchedAtEpochMillis
        }
    }
}

private fun readBackupDocuments(
    context: Context,
    uri: Uri
): List<BackupDocument> {
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        input.readLimited(MAX_BACKUP_BYTES)
    } ?: throw IllegalArgumentException("The selected file could not be opened.")

    val isZip = bytes.size >= 4 &&
        bytes[0] == 0x50.toByte() &&
        bytes[1] == 0x4B.toByte()

    if (!isZip) {
        return listOf(
            BackupDocument(
                name = uri.lastPathSegment ?: "tv-time-backup",
                content = bytes.toString(Charsets.UTF_8)
            )
        )
    }

    val documents = mutableListOf<BackupDocument>()
    ZipInputStream(ByteArrayInputStream(bytes)).use { zipInput ->
        var entry = zipInput.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && isSupportedTextEntry(entry.name)) {
                val entryBytes = zipInput.readLimited(MAX_TEXT_ENTRY_BYTES)
                documents += BackupDocument(
                    name = entry.name,
                    content = entryBytes.toString(Charsets.UTF_8)
                )
            }
            zipInput.closeEntry()
            entry = zipInput.nextEntry
        }
    }

    if (documents.isEmpty()) {
        throw IllegalArgumentException(
            "The ZIP does not contain readable JSON or CSV backup files."
        )
    }

    return documents
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
            throw IllegalArgumentException("The selected backup file is too large.")
        }
        output.write(buffer, 0, read)
    }

    return output.toByteArray()
}

private fun isSupportedTextEntry(name: String): Boolean {
    val lowerName = name.lowercase(Locale.ROOT)
    return lowerName.endsWith(".json") ||
        lowerName.endsWith(".csv") ||
        lowerName.endsWith(".txt")
}

private fun parseDocuments(documents: List<BackupDocument>): ParsedBackup {
    val parsedBackup = ParsedBackup()

    documents.forEach { document ->
        val trimmed = document.content.trimStart()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            runCatching {
                backupJson.parseToJsonElement(document.content)
            }.getOrNull()?.let { element ->
                visitJson(
                    element = element,
                    context = document.name,
                    parsedBackup = parsedBackup
                )
            }
        } else {
            parseDelimitedDocument(document, parsedBackup)
        }
    }

    return parsedBackup
}

private fun visitJson(
    element: JsonElement,
    context: String,
    parsedBackup: ParsedBackup
) {
    when (element) {
        is JsonObject -> {
            val flatValues = element.entries.mapNotNull { (key, value) ->
                primitiveText(value)?.let { text ->
                    normalizedKey(key) to text
                }
            }.toMap()

            val nestedShowTitle = nestedTitle(
                jsonObject = element,
                candidateKeys = setOf("show", "series", "program")
            )

            consumeRecord(
                values = flatValues,
                context = context,
                parsedBackup = parsedBackup,
                nestedShowTitle = nestedShowTitle
            )

            element.forEach { (key, child) ->
                visitJson(
                    element = child,
                    context = "$context/${normalizedKey(key)}",
                    parsedBackup = parsedBackup
                )
            }
        }

        is JsonArray -> {
            element.forEachIndexed { index, child ->
                visitJson(
                    element = child,
                    context = "$context/$index",
                    parsedBackup = parsedBackup
                )
            }
        }

        else -> Unit
    }
}

private fun nestedTitle(
    jsonObject: JsonObject,
    candidateKeys: Set<String>
): String? {
    jsonObject.forEach { (key, value) ->
        if (normalizedKey(key) !in candidateKeys || value !is JsonObject) {
            return@forEach
        }

        val title = value.entries.firstNotNullOfOrNull { (nestedKey, nestedValue) ->
            if (normalizedKey(nestedKey) in setOf("name", "title")) {
                primitiveText(nestedValue)
            } else {
                null
            }
        }
        if (!title.isNullOrBlank()) {
            return title
        }
    }
    return null
}

private fun primitiveText(element: JsonElement): String? {
    val primitive = element as? JsonPrimitive ?: return null
    return primitive.contentOrNull
}

private fun parseDelimitedDocument(
    document: BackupDocument,
    parsedBackup: ParsedBackup
) {
    val lines = document.content.lineSequence()
        .filter { line -> line.isNotBlank() }
        .toList()
    if (lines.size < 2) {
        return
    }

    val delimiter = listOf(',', ';', '\t').maxByOrNull { candidate ->
        lines.first().count { character -> character == candidate }
    } ?: ','
    val headers = parseDelimitedLine(lines.first(), delimiter)
        .map(::normalizedKey)

    lines.drop(1).forEach { line ->
        val values = parseDelimitedLine(line, delimiter)
        val record = headers.mapIndexedNotNull { index, key ->
            values.getOrNull(index)?.takeIf { it.isNotBlank() }?.let { value ->
                key to value
            }
        }.toMap()
        consumeRecord(
            values = record,
            context = document.name,
            parsedBackup = parsedBackup
        )
    }
}

private fun parseDelimitedLine(
    line: String,
    delimiter: Char
): List<String> {
    val values = mutableListOf<String>()
    val current = StringBuilder()
    var insideQuotes = false
    var index = 0

    while (index < line.length) {
        val character = line[index]
        when {
            character == '"' && insideQuotes &&
                index + 1 < line.length && line[index + 1] == '"' -> {
                current.append('"')
                index += 1
            }

            character == '"' -> insideQuotes = !insideQuotes
            character == delimiter && !insideQuotes -> {
                values += current.toString().trim()
                current.clear()
            }

            else -> current.append(character)
        }
        index += 1
    }

    values += current.toString().trim()
    return values
}

private fun consumeRecord(
    values: Map<String, String>,
    context: String,
    parsedBackup: ParsedBackup,
    nestedShowTitle: String? = null
) {
    if (values.isEmpty()) {
        return
    }

    val lowerContext = context.lowercase(Locale.ROOT)
    val seasonNumber = firstInt(
        values,
        "seasonnumber",
        "season",
        "seasonnum"
    )
    val episodeNumber = firstInt(
        values,
        "episodenumber",
        "episode",
        "episodenum"
    )
    val strongShowTitle = firstString(
        values,
        "showname",
        "showtitle",
        "seriesname",
        "seriestitle",
        "programname",
        "programtitle"
    )
    val generalTitle = firstString(values, "title", "name")
    val watchedAt = firstTimestamp(values)
    val status = firstString(
        values,
        "userstatus",
        "trackingstatus",
        "watchstatus",
        "status"
    )?.lowercase(Locale.ROOT).orEmpty()
    val stopped = status.contains("stopped") ||
        status.contains("paused") ||
        status.contains("abandoned") ||
        firstBoolean(values, "stopped", "paused") == true
    val completed = status.contains("completed") ||
        status.contains("finished") ||
        status.contains("allwatched")
    val explicitlyWatched = firstBoolean(
        values,
        "watched",
        "iswatched",
        "seen",
        "isseen",
        "completed"
    )
    val watchedByContext = lowerContext.contains("watched") ||
        lowerContext.contains("history") ||
        lowerContext.contains("seen")
    val isWatched = explicitlyWatched ?: watchedByContext

    if (seasonNumber != null && episodeNumber != null) {
        val showTitle = strongShowTitle ?: nestedShowTitle
        if (!showTitle.isNullOrBlank() && isWatched) {
            parsedBackup.addEpisode(
                showTitle = showTitle,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                watchedAtEpochMillis = watchedAt
            )
        }
        return
    }

    val isMovieContext = lowerContext.contains("movie") ||
        lowerContext.contains("film")
    if (isMovieContext) {
        val movieTitle = firstString(
            values,
            "movietitle",
            "moviename",
            "title",
            "name"
        )
        if (!movieTitle.isNullOrBlank()) {
            parsedBackup.addMovie(movieTitle, watchedAt)
        }
        return
    }

    val isShowContext = lowerContext.contains("show") ||
        lowerContext.contains("series") ||
        lowerContext.contains("follow") ||
        lowerContext.contains("tracking")
    val showTitle = strongShowTitle ?: nestedShowTitle ?: generalTitle
    if (isShowContext && !showTitle.isNullOrBlank()) {
        parsedBackup.addSeries(
            title = showTitle,
            stopped = stopped,
            completed = completed
        )
    }
}

private fun firstString(
    values: Map<String, String>,
    vararg keys: String
): String? = keys.firstNotNullOfOrNull { key ->
    values[key]?.trim()?.takeIf { value -> value.isNotBlank() }
}

private fun firstInt(
    values: Map<String, String>,
    vararg keys: String
): Int? = keys.firstNotNullOfOrNull { key ->
    values[key]?.trim()?.toIntOrNull()
}

private fun firstBoolean(
    values: Map<String, String>,
    vararg keys: String
): Boolean? = keys.firstNotNullOfOrNull { key ->
    when (values[key]?.trim()?.lowercase(Locale.ROOT)) {
        "true", "1", "yes", "watched", "seen", "completed" -> true
        "false", "0", "no", "unwatched", "not watched" -> false
        else -> null
    }
}

private fun firstTimestamp(values: Map<String, String>): Long? {
    val rawValue = firstString(
        values,
        "watchedat",
        "watcheddate",
        "seenat",
        "datewatched",
        "timestamp",
        "createdat",
        "date"
    ) ?: return null

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

    runCatching {
        return LocalDate.parse(rawValue)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    return null
}

private fun normalizedKey(value: String): String =
    value.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")

private fun cleanTitle(value: String): String =
    value.trim().replace(Regex("\\s+"), " ")

private fun normalizedMatchValue(value: String): String {
    val withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
    return withoutAccents
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]"), "")
}

private suspend fun findSeries(title: String): SeriesDto? {
    val results = runCatching {
        TmdbClient.api.searchSeries(query = title).results
    }.getOrDefault(emptyList())
    return bestSeriesMatch(title, results)
}

private suspend fun findMovie(title: String): MovieDto? {
    val results = runCatching {
        TmdbClient.api.searchMovies(query = title).results
    }.getOrDefault(emptyList())
    return bestMovieMatch(title, results)
}

private fun bestSeriesMatch(
    requestedTitle: String,
    candidates: List<SeriesDto>
): SeriesDto? {
    val normalizedRequested = normalizedMatchValue(requestedTitle)
    return candidates.firstOrNull { candidate ->
        normalizedMatchValue(candidate.name) == normalizedRequested
    } ?: candidates.firstOrNull()
}

private fun bestMovieMatch(
    requestedTitle: String,
    candidates: List<MovieDto>
): MovieDto? {
    val normalizedRequested = normalizedMatchValue(requestedTitle)
    return candidates.firstOrNull { candidate ->
        normalizedMatchValue(candidate.title) == normalizedRequested
    } ?: candidates.firstOrNull()
}
