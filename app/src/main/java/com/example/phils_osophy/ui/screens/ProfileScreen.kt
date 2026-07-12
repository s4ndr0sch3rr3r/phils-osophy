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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.phils_osophy.data.importer.TvTimeGdprImporter
import com.example.phils_osophy.data.local.BookStatus
import com.example.phils_osophy.data.local.SavedBookEntity
import com.example.phils_osophy.data.local.SavedGameEntity
import com.example.phils_osophy.data.local.SavedMovieEntity
import com.example.phils_osophy.data.local.SavedSeriesEntity
import com.example.phils_osophy.data.local.WatchedEpisodeEntity
import com.example.phils_osophy.data.remote.TmdbClient
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ProfileGreen = Color(0xFF64DD75)
private const val FallbackEpisodeRuntimeMinutes = 45

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
    val coroutineScope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var importFailed by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf<ProfileTimeStats?>(null) }
    var isLoadingStats by remember { mutableStateOf(true) }
    var statsRefreshToken by remember { mutableIntStateOf(0) }

    val backupPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            isImporting = true
            importMessage = null
            importFailed = false

            try {
                val result = TvTimeGdprImporter.importBackup(
                    context = context,
                    uri = uri
                )
                importMessage = result.summary()
                statsRefreshToken += 1
            } catch (exception: Exception) {
                importFailed = true
                importMessage = exception.localizedMessage
                    ?: "The TV Time backup could not be imported."
            } finally {
                isImporting = false
            }
        }
    }

    LaunchedEffect(
        movies,
        series,
        watchedEpisodes,
        games,
        books,
        statsRefreshToken
    ) {
        isLoadingStats = true
        stats = loadProfileTimeStats(
            movies = movies,
            series = series,
            watchedEpisodes = watchedEpisodes,
            games = games,
            books = books
        )
        isLoadingStats = false
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

                TextButton(
                    onClick = {
                        statsRefreshToken += 1
                    },
                    enabled = !isLoadingStats
                ) {
                    Text("Refresh")
                }
            }
        }

        if (isLoadingStats && stats == null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            stats?.let { currentStats ->
                item {
                    ProfileTimeCard(
                        title = "All tracked media",
                        value = formatDuration(currentStats.totalMinutes),
                        detail = "Movies, watched series episodes and games",
                        highlighted = true
                    )
                }

                item {
                    ProfileTimeCard(
                        title = "Movies",
                        value = formatDuration(currentStats.movieMinutes),
                        detail = "${currentStats.movieCount} saved movies · TMDB runtimes"
                    )
                }

                item {
                    ProfileTimeCard(
                        title = "Series",
                        value = formatDuration(currentStats.seriesMinutes),
                        detail = "${currentStats.watchedEpisodeCount} watched episodes · estimated from TMDB runtimes"
                    )
                }

                item {
                    ProfileTimeCard(
                        title = "Games",
                        value = formatDuration(currentStats.gameMinutes),
                        detail = "${currentStats.gameCount} saved games · manually tracked playtime"
                    )
                }

                item {
                    ProfileTimeCard(
                        title = "Books",
                        value = "Time not tracked",
                        detail = "${currentStats.finishedBookCount}/${currentStats.bookCount} books finished. " +
                            "Reading duration is not stored, so it is excluded from the total."
                    )
                }
            }
        }

        item {
            TvTimeBackupCard(
                isImporting = isImporting,
                importMessage = importMessage,
                importFailed = importFailed,
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
                text = "For every imported series, all episodes from season 1 " +
                    "through the latest watched episode are marked watched.",
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
    val finishedBookCount: Int
) {
    val totalMinutes: Long
        get() = movieMinutes + seriesMinutes + gameMinutes
}

private suspend fun loadProfileTimeStats(
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
        } catch (_: Exception) {
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
        } catch (_: Exception) {
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
        finishedBookCount = finishedBooks
    )
}

private fun formatDuration(totalMinutes: Long): String {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60

    return when {
        hours == 0L -> "$minutes min"
        minutes == 0L -> "$hours h"
        else -> "$hours h $minutes min"
    }
}
