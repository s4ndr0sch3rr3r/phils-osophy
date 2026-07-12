package com.example.phils_osophy.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.phils_osophy.data.importer.TvTimeImportManager
import com.example.phils_osophy.data.local.BookStatus
import com.example.phils_osophy.data.local.PhilsOsophyDatabase
import com.example.phils_osophy.data.local.ProfileStatsCacheEntity
import com.example.phils_osophy.data.local.SavedBookEntity
import com.example.phils_osophy.data.local.SavedGameEntity
import com.example.phils_osophy.data.local.SavedMovieEntity
import com.example.phils_osophy.data.local.SavedSeriesEntity
import com.example.phils_osophy.data.local.WatchedEpisodeEntity
import com.example.phils_osophy.data.remote.TmdbClient
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val ProfileGreen = Color(0xFF64DD75)
private const val FallbackEpisodeRuntimeMinutes = 45
private const val MinutesPerHour = 60L
private const val MinutesPerDay = 24L * MinutesPerHour
private const val MinutesPerMonth = 30L * MinutesPerDay
private val StatisticsDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")

@Composable
fun ProfileScreen(
    movies: List<SavedMovieEntity>,
    series: List<SavedSeriesEntity>,
    watchedEpisodes: List<WatchedEpisodeEntity>,
    games: List<SavedGameEntity>,
    books: List<SavedBookEntity>,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current.applicationContext
    val database = remember(context) {
        PhilsOsophyDatabase.getInstance(context)
    }
    val statisticsDao = remember(database) {
        database.profileStatsCacheDao()
    }
    val importState by TvTimeImportManager.state.collectAsState()

    var statistics by remember {
        mutableStateOf(ProfileTimeStats.empty())
    }
    var cacheLoaded by remember { mutableStateOf(false) }
    var isRefreshingStatistics by remember { mutableStateOf(false) }
    var statisticsRefreshMessage by remember { mutableStateOf<String?>(null) }
    var statisticsRefreshToken by remember { mutableIntStateOf(0) }

    val backupPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            TvTimeImportManager.start(
                context = context,
                uri = uri
            )
        }
    }

    LaunchedEffect(importState.completedAtEpochMillis) {
        if (
            importState.completedAtEpochMillis > 0 &&
            !importState.failed
        ) {
            statisticsRefreshToken += 1
        }
    }

    LaunchedEffect(statisticsDao) {
        val cachedSnapshot = withContext(Dispatchers.IO) {
            statisticsDao.getSnapshot()
                ?: ProfileStatsCacheEntity()
        }
        statistics = cachedSnapshot.toProfileTimeStats()
        cacheLoaded = true
    }

    LaunchedEffect(
        cacheLoaded,
        movies,
        series,
        watchedEpisodes,
        games,
        books,
        statisticsRefreshToken
    ) {
        if (!cacheLoaded) {
            return@LaunchedEffect
        }

        delay(300)
        isRefreshingStatistics = true
        statisticsRefreshMessage = null

        try {
            val refreshedStatistics = calculateProfileTimeStats(
                movies = movies,
                series = series,
                watchedEpisodes = watchedEpisodes,
                games = games,
                books = books
            )
            val completedAt = System.currentTimeMillis()
            val completedSnapshot = refreshedStatistics.copy(
                calculatedAtEpochMillis = completedAt
            )

            withContext(Dispatchers.IO) {
                statisticsDao.replaceSnapshot(
                    completedSnapshot.toCacheEntity()
                )
            }

            statistics = completedSnapshot
        } catch (_: Exception) {
            statisticsRefreshMessage =
                "Refresh failed. Showing the last saved statistics."
        } finally {
            isRefreshingStatistics = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBackClick) {
                    Text("← Back")
                }

                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total time",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                if (isRefreshingStatistics) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }

                TextButton(
                    onClick = {
                        statisticsRefreshToken += 1
                    },
                    enabled = !isRefreshingStatistics
                ) {
                    Text(
                        if (isRefreshingStatistics) {
                            "Updating…"
                        } else {
                            "Refresh"
                        }
                    )
                }
            }
        }

        item {
            ProfileTimeCard(
                title = "All tracked media",
                value = formatDuration(statistics.totalMinutes),
                detail = statisticsSummaryDetail(statistics),
                highlighted = true
            )
        }

        item {
            ProfileTimeCard(
                title = "Movies",
                value = formatDuration(statistics.movieMinutes),
                detail = buildString {
                    append("${statistics.movieCount} saved movies · TMDB runtimes")
                    if (isRefreshingStatistics) {
                        append(" · updating in background")
                    }
                }
            )
        }

        item {
            ProfileTimeCard(
                title = "Series",
                value = formatDuration(statistics.seriesMinutes),
                detail = "${statistics.watchedEpisodeCount} watched episodes · " +
                    "estimated from TMDB runtimes"
            )
        }

        item {
            ProfileTimeCard(
                title = "Games",
                value = formatDuration(statistics.gameMinutes),
                detail = "${statistics.gameCount} saved games · manually tracked playtime"
            )
        }

        item {
            ProfileTimeCard(
                title = "Books",
                value = "Time not tracked",
                detail = "${statistics.finishedBookCount}/${statistics.bookCount} books finished. " +
                    "Reading duration is not stored, so it is excluded from the total."
            )
        }

        statisticsRefreshMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            TvTimeBackupCard(
                isImporting = importState.isRunning,
                importMessage = importState.message,
                importFailed = importState.failed,
                onChooseFile = {
                    backupPicker.launch(
                        arrayOf(
                            "application/zip",
                            "application/octet-stream"
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun TvTimeBackupCard(
    isImporting: Boolean,
    importMessage: String?,
    importFailed: Boolean,
    onChooseFile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Import TV Time history",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Choose the file normally downloaded from WhatsApp " +
                    "or TV Time. Only watched movies and series are imported.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = "gdpr-data.zip",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                singleLine = true,
                label = {
                    Text("Backup filename")
                },
                supportingText = {
                    Text("Usually located in Downloads or WhatsApp Documents")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onChooseFile,
                enabled = !isImporting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Importing…")
                } else {
                    Text("Find gdpr-data.zip with file manager")
                }
            }

            Text(
                text = "The import keeps running when you switch screens. " +
                    "Return to Profile to see the result and refreshed database values.",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            importMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = if (importFailed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        ProfileGreen
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ProfileTimeCard(
    title: String,
    value: String,
    detail: String,
    highlighted: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                Color(0xFF123C1A)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (highlighted) {
                    ProfileGreen
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class ProfileTimeStats(
    val movieMinutes: Long,
    val seriesMinutes: Long,
    val gameMinutes: Long,
    val movieCount: Int,
    val watchedEpisodeCount: Int,
    val gameCount: Int,
    val bookCount: Int,
    val finishedBookCount: Int,
    val calculatedAtEpochMillis: Long
) {
    val totalMinutes: Long
        get() = movieMinutes + seriesMinutes + gameMinutes

    companion object {
        fun empty(): ProfileTimeStats = ProfileTimeStats(
            movieMinutes = 0,
            seriesMinutes = 0,
            gameMinutes = 0,
            movieCount = 0,
            watchedEpisodeCount = 0,
            gameCount = 0,
            bookCount = 0,
            finishedBookCount = 0,
            calculatedAtEpochMillis = 0
        )
    }
}

private suspend fun calculateProfileTimeStats(
    movies: List<SavedMovieEntity>,
    series: List<SavedSeriesEntity>,
    watchedEpisodes: List<WatchedEpisodeEntity>,
    games: List<SavedGameEntity>,
    books: List<SavedBookEntity>
): ProfileTimeStats = withContext(Dispatchers.IO) {
    var movieMinutes = 0L
    movies.forEach { movie ->
        val runtime = try {
            TmdbClient.api.getMovieDetails(movie.id).runtime ?: 0
        } catch (exception: Exception) {
            if (exception is kotlinx.coroutines.CancellationException) {
                throw exception
            }
            0
        }
        movieMinutes += runtime.coerceAtLeast(0).toLong()
    }

    val watchedBySeries = watchedEpisodes.groupingBy { episode ->
        episode.seriesId
    }.eachCount()
    val knownSeriesIds = series.map { savedSeries -> savedSeries.id }.toSet()
    var seriesMinutes = 0L

    watchedBySeries.forEach { (seriesId, watchedCount) ->
        if (seriesId !in knownSeriesIds) {
            return@forEach
        }

        val averageRuntime = try {
            val runtimes = TmdbClient.api.getSeriesDetails(seriesId)
                .episodeRunTime
                .filter { runtime -> runtime > 0 }
            if (runtimes.isEmpty()) {
                FallbackEpisodeRuntimeMinutes
            } else {
                runtimes.average().roundToInt()
            }
        } catch (exception: Exception) {
            if (exception is kotlinx.coroutines.CancellationException) {
                throw exception
            }
            FallbackEpisodeRuntimeMinutes
        }

        seriesMinutes += watchedCount.toLong() * averageRuntime
    }

    val gameMinutes = games.sumOf { game ->
        game.hoursPlayedMinutes.coerceAtLeast(0).toLong()
    }
    val finishedBooks = books.count { book ->
        BookStatus.fromStorage(book.status) == BookStatus.FINISHED
    }

    ProfileTimeStats(
        movieMinutes = movieMinutes,
        seriesMinutes = seriesMinutes,
        gameMinutes = gameMinutes,
        movieCount = movies.size,
        watchedEpisodeCount = watchedEpisodes.size,
        gameCount = games.size,
        bookCount = books.size,
        finishedBookCount = finishedBooks,
        calculatedAtEpochMillis = 0
    )
}

private fun ProfileStatsCacheEntity.toProfileTimeStats(): ProfileTimeStats =
    ProfileTimeStats(
        movieMinutes = movieMinutes,
        seriesMinutes = seriesMinutes,
        gameMinutes = gameMinutes,
        movieCount = movieCount,
        watchedEpisodeCount = watchedEpisodeCount,
        gameCount = gameCount,
        bookCount = bookCount,
        finishedBookCount = finishedBookCount,
        calculatedAtEpochMillis = calculatedAtEpochMillis
    )

private fun ProfileTimeStats.toCacheEntity(): ProfileStatsCacheEntity =
    ProfileStatsCacheEntity(
        movieMinutes = movieMinutes,
        seriesMinutes = seriesMinutes,
        gameMinutes = gameMinutes,
        movieCount = movieCount,
        watchedEpisodeCount = watchedEpisodeCount,
        gameCount = gameCount,
        bookCount = bookCount,
        finishedBookCount = finishedBookCount,
        calculatedAtEpochMillis = calculatedAtEpochMillis
    )

private fun statisticsSummaryDetail(statistics: ProfileTimeStats): String {
    if (statistics.calculatedAtEpochMillis <= 0) {
        return "Saved snapshot · waiting for the first completed calculation"
    }

    val formattedDate = Instant.ofEpochMilli(statistics.calculatedAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .format(StatisticsDateFormatter)
    return "Movies, watched series episodes and games · updated $formattedDate"
}

private fun formatDuration(totalMinutes: Long): String {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    val months = safeMinutes / MinutesPerMonth
    val minutesAfterMonths = safeMinutes % MinutesPerMonth
    val days = minutesAfterMonths / MinutesPerDay
    val minutesAfterDays = minutesAfterMonths % MinutesPerDay
    val hours = minutesAfterDays / MinutesPerHour
    val minutes = minutesAfterDays % MinutesPerHour

    return when {
        months > 0L -> formatDurationPair(months, "mo", days, "d")
        days > 0L -> formatDurationPair(days, "d", hours, "h")
        hours > 0L -> formatDurationPair(hours, "h", minutes, "min")
        else -> "$minutes min"
    }
}

private fun formatDurationPair(
    primaryValue: Long,
    primaryUnit: String,
    secondaryValue: Long,
    secondaryUnit: String
): String = if (secondaryValue > 0L) {
    "$primaryValue $primaryUnit $secondaryValue $secondaryUnit"
} else {
    "$primaryValue $primaryUnit"
}
