package com.example.phils_osophy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phils_osophy.data.local.PhilsOsophyDatabase
import com.example.phils_osophy.data.local.SeriesCompletionTracker
import com.example.phils_osophy.data.remote.EpisodeDto
import com.example.phils_osophy.data.remote.TmdbClient
import com.example.phils_osophy.ui.util.formatStoredDate
import kotlinx.coroutines.launch

private const val TMDB_EPISODE_DETAIL_IMAGE_BASE_URL =
    "https://image.tmdb.org/t/p/w780"
private val EpisodeWatchedGreen = Color(0xFF55C900)
private val EpisodeUnwatchedGrey = Color(0xFFE6E6E6)

@Composable
fun EpisodeDetailScreen(
    seriesId: Int,
    seriesName: String,
    seasonNumber: Int,
    episodeNumber: Int,
    isWatched: Boolean,
    onBackClick: () -> Unit,
    onWatchedChange: (Boolean) -> Unit
) {
    var episode by remember(seriesId, seasonNumber, episodeNumber) {
        mutableStateOf<EpisodeDto?>(null)
    }
    var isLoading by remember(seriesId, seasonNumber, episodeNumber) {
        mutableStateOf(true)
    }
    var errorMessage by remember(seriesId, seasonNumber, episodeNumber) {
        mutableStateOf<String?>(null)
    }

    val applicationContext = LocalContext.current.applicationContext
    val database = remember(applicationContext) {
        PhilsOsophyDatabase.getInstance(applicationContext)
    }
    val watchedEpisodeFlow = remember(database, seriesId) {
        database.watchedEpisodeDao().observeForSeries(seriesId)
    }
    val watchedEpisodeRecords by watchedEpisodeFlow.collectAsState(
        initial = emptyList()
    )
    val watchedAtEpochMillis = watchedEpisodeRecords
        .firstOrNull { record ->
            record.seasonNumber == seasonNumber &&
                record.episodeNumber == episodeNumber
        }
        ?.watchedAtEpochMillis
    val completionTracker = remember(applicationContext) {
        SeriesCompletionTracker(applicationContext)
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(seriesId, seasonNumber, episodeNumber) {
        isLoading = true
        errorMessage = null

        try {
            episode = TmdbClient.api.getEpisodeDetails(
                seriesId = seriesId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )
        } catch (exception: Exception) {
            errorMessage = exception.localizedMessage
                ?: "Episode information could not be loaded."
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val loadedEpisode = episode
    val imageUrl = loadedEpisode
        ?.stillPath
        ?.let { path -> "$TMDB_EPISODE_DETAIL_IMAGE_BASE_URL$path" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = loadedEpisode?.name
                        ?: "Episode image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            TextButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 8.dp, top = 8.dp)
            ) {
                Text(
                    text = "← Back",
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = seriesName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "S%02d | E%02d".format(
                        seasonNumber,
                        episodeNumber
                    ),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = loadedEpisode
                        ?.name
                        ?.takeIf { name -> name.isNotBlank() }
                        ?: "Episode $episodeNumber",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Air date",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = loadedEpisode
                        ?.airDate
                        ?.takeIf { date -> date.isNotBlank() }
                        ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Runtime",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = loadedEpisode
                        ?.runtime
                        ?.takeIf { runtime -> runtime > 0 }
                        ?.let { runtime -> "$runtime min" }
                        ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (isWatched) {
                            EpisodeWatchedGreen
                        } else {
                            EpisodeUnwatchedGrey
                        },
                        shape = CircleShape
                    )
                    .clickable {
                        val watched = !isWatched
                        onWatchedChange(watched)
                        coroutineScope.launch {
                            completionTracker.setEpisodeWatched(
                                seriesId = seriesId,
                                seasonNumber = seasonNumber,
                                episodeNumber = episodeNumber,
                                watched = watched
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = if (isWatched) {
                        Color.White
                    } else {
                        Color.Gray
                    },
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier.padding(
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp
            )
        ) {
            Text(
                text = "Watched",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isWatched) {
                    formatStoredDate(watchedAtEpochMillis)
                } else {
                    "Not watched"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }

        HorizontalDivider()

        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Episode information",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = loadedEpisode
                        ?.overview
                        ?.takeIf { overview -> overview.isNotBlank() }
                        ?: "No episode information available.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
