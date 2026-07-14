package com.example.phils_osophy.data.importer

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.phils_osophy.data.local.PhilsOsophyDatabase
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.Normalizer
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LOCAL_SERIES_HISTORY_FILE = "tracking-prod-records-v2.csv"
private const val LOCAL_MOVIE_HISTORY_FILE = "tracking-prod-records.csv"
private const val LOCAL_MAX_ENTRY_BYTES = 32 * 1024 * 1024
private const val LOCAL_IMPORT_LOG_TAG = "TvTimeGdprImport"

private val LocalRequiredHistoryFiles = listOf(
    LOCAL_SERIES_HISTORY_FILE,
    LOCAL_MOVIE_HISTORY_FILE
)

private val LocalWatchedMovieActions = setOf("watch", "rewatch_count")
private val LocalTrailingReleaseYearPattern =
    Regex("""\s*(?:\(\d{4}\)|\[\d{4}])\s*$""")

object LocalAwareTvTimeGdprImporter {

    suspend fun importBackup(
        context: Context,
        uri: Uri
    ): TvTimeImportResult = withContext(Dispatchers.IO) {
        val savedMovieKeys = PhilsOsophyDatabase.getInstance(context)
            .savedMovieDao()
            .getAll()
            .asSequence()
            .map { movie -> normalizedLocalMovieKey(movie.title) }
            .filter { key -> key.isNotEmpty() }
            .toSet()

        if (savedMovieKeys.isEmpty()) {
            return@withContext ParallelTvTimeGdprImporter.importBackup(context, uri)
        }

        val filteredBackup = createLocalFilteredBackup(
            context = context,
            uri = uri,
            savedMovieKeys = savedMovieKeys
        )
        if (filteredBackup.existingMovieCount == 0) {
            filteredBackup.file.delete()
            return@withContext ParallelTvTimeGdprImporter.importBackup(context, uri)
        }

        try {
            Log.i(
                LOCAL_IMPORT_LOG_TAG,
                "Skipping ${filteredBackup.existingMovieCount} Movies already saved locally"
            )
            ParallelTvTimeGdprImporter.importBackup(
                context = context,
                uri = Uri.fromFile(filteredBackup.file)
            )
        } finally {
            filteredBackup.file.delete()
        }
    }
}

private data class LocalFilteredBackup(
    val file: File,
    val existingMovieCount: Int
)

private data class LocalFilteredMovieCsv(
    val content: String,
    val existingMovieCount: Int
)

private fun createLocalFilteredBackup(
    context: Context,
    uri: Uri,
    savedMovieKeys: Set<String>
): LocalFilteredBackup {
    val entries = readLocalRequiredEntries(context, uri)
    val filteredMovies = filterLocalMovieCsv(
        content = entries.getValue(LOCAL_MOVIE_HISTORY_FILE).toString(Charsets.UTF_8),
        savedMovieKeys = savedMovieKeys
    )
    val file = File.createTempFile("tv-time-gdpr-local-", ".zip", context.cacheDir)

    try {
        ZipOutputStream(file.outputStream().buffered()).use { output ->
            LocalRequiredHistoryFiles.forEach { name ->
                output.putNextEntry(ZipEntry(name))
                val bytes = if (name == LOCAL_MOVIE_HISTORY_FILE) {
                    filteredMovies.content.toByteArray(Charsets.UTF_8)
                } else {
                    entries.getValue(name)
                }
                output.write(bytes)
                output.closeEntry()
            }
        }
    } catch (exception: Exception) {
        file.delete()
        throw exception
    }

    return LocalFilteredBackup(
        file = file,
        existingMovieCount = filteredMovies.existingMovieCount
    )
}

private fun readLocalRequiredEntries(
    context: Context,
    uri: Uri
): Map<String, ByteArray> {
    val input = context.contentResolver.openInputStream(uri)
        ?: throw IllegalArgumentException("The selected file could not be opened.")
    val entries = linkedMapOf<String, ByteArray>()

    try {
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.substringAfterLast('/').lowercase(Locale.ROOT)
                if (!entry.isDirectory && name in LocalRequiredHistoryFiles) {
                    entries[name] = zip.readLocalLimited(LOCAL_MAX_ENTRY_BYTES)
                }
                zip.closeEntry()
            }
        }
    } catch (_: ZipException) {
        throw IllegalArgumentException("Select the TV Time file named gdpr-data.zip.")
    }

    val missing = LocalRequiredHistoryFiles.toSet() - entries.keys
    if (missing.isNotEmpty()) {
        throw IllegalArgumentException(
            "gdpr-data.zip is missing: ${missing.sorted().joinToString()}."
        )
    }
    return entries
}

private fun filterLocalMovieCsv(
    content: String,
    savedMovieKeys: Set<String>
): LocalFilteredMovieCsv {
    val lines = content.lineSequence().toList()
    if (lines.size < 2) {
        return LocalFilteredMovieCsv(content, 0)
    }

    val headers = parseLocalCsvLine(lines.first()).map(::normalizedLocalHeader)
    val entityTypeIndex = headers.indexOf("entitytype")
    val actionIndex = headers.indexOf("type")
    val movieNameIndex = headers.indexOf("moviename")
    if (entityTypeIndex < 0 || actionIndex < 0 || movieNameIndex < 0) {
        return LocalFilteredMovieCsv(content, 0)
    }

    val matchedMovieKeys = mutableSetOf<String>()
    val retainedLines = ArrayList<String>(lines.size)
    retainedLines += lines.first()

    lines.drop(1).forEach { line ->
        val values = parseLocalCsvLine(line)
        val entityType = values.getOrNull(entityTypeIndex)?.trim()?.lowercase(Locale.ROOT)
        val action = values.getOrNull(actionIndex)?.trim()?.lowercase(Locale.ROOT)
        val movieKey = values.getOrNull(movieNameIndex)
            ?.let(::normalizedLocalMovieKey)
            .orEmpty()
        val alreadySaved = entityType == "movie" &&
            action in LocalWatchedMovieActions &&
            movieKey.isNotEmpty() &&
            movieKey in savedMovieKeys

        if (alreadySaved) {
            matchedMovieKeys += movieKey
        } else {
            retainedLines += line
        }
    }

    val filteredContent = buildString {
        append(retainedLines.joinToString("\n"))
        if (content.endsWith('\n')) {
            append('\n')
        }
    }
    return LocalFilteredMovieCsv(
        content = filteredContent,
        existingMovieCount = matchedMovieKeys.size
    )
}

private fun parseLocalCsvLine(line: String): List<String> {
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

private fun normalizedLocalHeader(value: String): String =
    value.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")

private fun normalizedLocalMovieKey(value: String): String {
    val withoutReleaseYear = value.replace(LocalTrailingReleaseYearPattern, "")
    val withoutAccents = Normalizer.normalize(withoutReleaseYear, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
    return withoutAccents.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
}

private fun InputStream.readLocalLimited(maxBytes: Int): ByteArray {
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
