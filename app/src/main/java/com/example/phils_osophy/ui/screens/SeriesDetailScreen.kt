package com.example.phils_osophy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phils_osophy.data.local.PhilsOsophyDatabase
import com.example.phils_osophy.data.local.SavedSeriesEntity
import com.example.phils_osophy.data.local.SeriesCompletionTracker
import com.example.phils_osophy.data.local.WatchedEpisodeKey
import com.example.phils_osophy.data.remote.EpisodeDto
import com.example.phils_osophy.data.remote.SeasonDetailsDto
import com.example.phils_osophy.data.remote.SeasonSummaryDto
import com.example.phils_osophy.data.remote.SeriesDetailsDto
import com.example.phils_osophy.data.remote.TmdbClient
import com.example.phils_osophy.ui.util.formatStoredDate
import kotlinx.coroutines.launch

private const val TMDB_SERIES_BACKDROP_BASE_URL =
    "https://image.tmdb.org/t/p/w780"
private const val TMDB_EPISODE_STILL_BASE_URL =
    "https://image.tmdb.org/t/p/w500"

private val WatchedGreen = Color(0xFF55C900)
private val UnwatchedGrey = Color(0xFFE6E6E6)

@Composable
fun SeriesDetailScreen(
    series: SavedSeriesEntity,
    watchedEpisodes: Set<WatchedEpisodeKey>,
    onBackClick: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit,
    onChangeRating: (Int) -> Unit,
    onEpisodeClick: (
        seasonNumber: Int,
        episodeNumber: Int
    ) -> Unit,
    onWatchedChange: (
        seasonNumber: Int,
        episodeNumber: Int,
        watched: Boolean
    ) -> Unit
) {
    var details by remember(series.id) {
        mutableStateOf<SeriesDetailsDto?>(null)
    }
    var isLoading by remember(series.id) {
        mutableStateOf(true)
    }
    var errorMessage by remember(series.id) {
        mutableStateOf<String?>(null)
    }
    var selectedTab by remember(series.id) {
        mutableStateOf(0)
    }
    var expandedSeasonNumber by remember(series.id) {
        mutableStateOf<Int?>(null)
    }
    var loadingSeasonNumber by remember(series.id) {
        mutableStateOf<Int?>(null)
    }
    var isRatingDialogVisible by remember(series.id) {
        mutableStateOf(false)
    }

    val seasonCache = remember(series.id) {
        mutableStateMapOf<Int, SeasonDetailsDto>()
    }
    val applicationContext = LocalContext.current.applicationContext
    val database = remember(applicationContext) {
        PhilsOsophyDatabase.getInstance(applicationContext)
    }
    val completionTracker = remember(applicationContext) {
        SeriesCompletionTracker(applicationContext)
    }
    val watchedEpisodeFlow = remember(database, series.id) {
        database.watchedEpisodeDao().observeForSeries(series.id)
    }
    val watchedEpisodeRecords by watchedEpisodeFlow.collectAsState(
        initial = emptyList()
    )
    val watchedEpisodeDates = watchedEpisodeRecords.associate { record ->
        WatchedEpisodeKey(
            seasonNumber = record.seasonNumber,
            episodeNumber = record.episodeNumber
        ) to record.watchedAtEpochMillis
    }
    val effectiveWatchedEpisodes = watchedEpisodes + watchedEpisodeDates.keys
    val seriesCompletedAtEpochMillis = seriesCompletionTimestamp(
        details = details,
        watchedEpisodeDates = watchedEpisodeDates
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(series.id) {
        isLoading = true
        errorMessage = null

        try {
            details = TmdbClient.api.getSeriesDetails(series.id)
            expandedSeasonNumber = details
                ?.seasons
                .orEmpty()
                .firstOrNull { season -> season.seasonNumber > 0 }
                ?.seasonNumber
        } catch (exception: Exception) {
            errorMessage = exception.localizedMessage
                ?: "Series details could not be loaded."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(expandedSeasonNumber) {
        val seasonNumber = expandedSeasonNumber ?: return@LaunchedEffect

        if (seasonCache[seasonNumber] == null) {
            loadingSeasonNumber = seasonNumber
            try {
                seasonCache[seasonNumber] = TmdbClient.api.getSeasonDetails(
                    seriesId = series.id,
                    seasonNumber = seasonNumber
                )
            } catch (_: Exception) {
                // Collapse and reopen the season to retry.
            } finally {
                loadingSeasonNumber = null
            }
        }
    }

    val title = details
        ?.name
        ?.takeIf { name -> name.isNotBlank() }
        ?: series.name
    val imagePath = details?.backdropPath
        ?: details?.posterPath
        ?: series.posterPath
    val imageUrl = imagePath?.let { path ->
        "$TMDB_SERIES_BACKDROP_BASE_URL$path"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SeriesHero(
            title = title,
            subtitle = seriesSubtitle(details),
            imageUrl = imageUrl,
            isFavorite = series.isFavorite,
            onBackClick = onBackClick,
            onFavoriteClick = onFavoriteClick
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Info") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Episodes") }
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            selectedTab == 0 -> {
                SeriesInfoTab(
                    series = series,
                    details = details,
                    completedAtEpochMillis = seriesCompletedAtEpochMillis,
                    errorMessage = errorMessage,
                    onChangeRatingClick = { isRatingDialogVisible = true },
                    onSaveComment = { comment ->
                        coroutineScope.launch {
                            database.savedSeriesDao().updateNote(
                                seriesId = series.id,
                                note = comment
                            )
                        }
                    }
                )
            }

            else -> {
                EpisodesTab(
                    seasons = details
                        ?.seasons
                        .orEmpty()
                        .filter { season -> season.seasonNumber > 0 },
                    expandedSeasonNumber = expandedSeasonNumber,
                    loadingSeasonNumber = loadingSeasonNumber,
                    seasonCache = seasonCache,
                    watchedEpisodes = effectiveWatchedEpisodes,
                    watchedEpisodeDates = watchedEpisodeDates,
                    onSeasonClick = { seasonNumber ->
                        expandedSeasonNumber =
                            if (expandedSeasonNumber == seasonNumber) {
                                null
                            } else {
                                seasonNumber
                            }
                    },
                    onSeasonWatchedClick = { seasonNumber, episodeCount ->
                        coroutineScope.launch {
                            completionTracker.markSeasonWatched(
                                seriesId = series.id,
                                seasonNumber = seasonNumber,
                                episodeCount = episodeCount
                            )
                        }
                    },
                    onEpisodeClick = onEpisodeClick,
                    onWatchedChange = { seasonNumber, episodeNumber, watched ->
                        onWatchedChange(
                            seasonNumber,
                            episodeNumber,
                            watched
                        )
                        coroutineScope.launch {
                            completionTracker.setEpisodeWatched(
                                seriesId = series.id,
                                seasonNumber = seasonNumber,
                                episodeNumber = episodeNumber,
                                watched = watched
                            )
                        }
                    }
                )
            }
        }
    }

    if (isRatingDialogVisible) {
        UserRatingDialog(
            title = "Rate ${series.name}",
            initialRating = series.userRating,
            onSave = { rating ->
                onChangeRating(rating)
                isRatingDialogVisible = false
            },
            onCancel = { isRatingDialogVisible = false }
        )
    }
}

@Composable
private fun SeriesHero(
    title: String,
    subtitle: String,
    imageUrl: String?,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit
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
                contentDescription = "$title backdrop",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
            Text("← Back", color = Color.White)
        }

        TextButton(
            onClick = { onFavoriteClick(!isFavorite) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 8.dp, top = 8.dp)
        ) {
            Text(
                text = if (isFavorite) "♥" else "♡",
                color = if (isFavorite) Color(0xFFE53935) else Color.White,
                fontSize = 30.sp
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun SeriesInfoTab(
    series: SavedSeriesEntity,
    details: SeriesDetailsDto?,
    completedAtEpochMillis: Long?,
    errorMessage: String?,
    onChangeRatingClick: () -> Unit,
    onSaveComment: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Series information",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item {
            Text(
                text = details
                    ?.overview
                    ?.takeIf { overview -> overview.isNotBlank() }
                    ?: series.overview.ifBlank {
                        "No series information available."
                    },
                style = MaterialTheme.typography.bodyLarge
            )
        }

        item { HorizontalDivider() }

        item {
            DetailValue(
                label = "Your rating",
                value = if (series.userRating in 1..USER_RATING_MAX) {
                    "${series.userRating} / $USER_RATING_MAX"
                } else {
                    "Not rated"
                }
            )
            TextButton(onClick = onChangeRatingClick) {
                Text("Change rating")
            }
        }

        item {
            DetailValue(
                label = "First aired",
                value = details
                    ?.firstAirDate
                    ?.takeIf { date -> date.isNotBlank() }
                    ?: series.firstAirDate.ifBlank { "Unknown" }
            )
        }

        item {
            DetailValue(
                label = "Rating",
                value = details
                    ?.voteAverage
                    ?.takeIf { rating -> rating > 0.0 }
                    ?.let { rating -> "%.1f / 10".format(rating) }
                    ?: if (series.voteAverage > 0.0) {
                        "%.1f / 10".format(series.voteAverage)
                    } else {
                        "Not rated"
                    }
            )
        }

        item {
            DetailValue(
                label = "Seasons",
                value = details
                    ?.numberOfSeasons
                    ?.takeIf { count -> count > 0 }
                    ?.toString()
                    ?: "Unknown"
            )
        }

        item {
            DetailValue(
                label = "Episodes",
                value = details
                    ?.numberOfEpisodes
                    ?.takeIf { count -> count > 0 }
                    ?.toString()
                    ?: "Unknown"
            )
        }

        item {
            DetailValue(
                label = "Added to app",
                value = formatStoredDate(series.addedAtEpochMillis)
            )
        }

        item {
            DetailValue(
                label = "Completed",
                value = completedAtEpochMillis
                    ?.let(::formatStoredDate)
                    ?: "Not completed"
            )
        }

        item {
            DetailValue(
                label = "Status",
                value = details
                    ?.status
                    ?.takeIf { status -> status.isNotBlank() }
                    ?: "Unknown"
            )
        }

        item { HorizontalDivider() }

        item {
            EditableMediaCommentSection(
                mediaKey = series.id,
                savedComment = series.note,
                onSave = onSaveComment,
                contentPadding = 0.dp
            )
        }
    }
}

@Composable
private fun DetailValue(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun EpisodesTab(
    seasons: List<SeasonSummaryDto>,
    expandedSeasonNumber: Int?,
    loadingSeasonNumber: Int?,
    seasonCache: Map<Int, SeasonDetailsDto>,
    watchedEpisodes: Set<WatchedEpisodeKey>,
    watchedEpisodeDates: Map<WatchedEpisodeKey, Long>,
    onSeasonClick: (Int) -> Unit,
    onSeasonWatchedClick: (
        seasonNumber: Int,
        episodeCount: Int
    ) -> Unit,
    onEpisodeClick: (
        seasonNumber: Int,
        episodeNumber: Int
    ) -> Unit,
    onWatchedChange: (
        seasonNumber: Int,
        episodeNumber: Int,
        watched: Boolean
    ) -> Unit
) {
    if (seasons.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No episode information available.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = seasons,
            key = { season -> season.seasonNumber }
        ) { season ->
            val isExpanded = expandedSeasonNumber == season.seasonNumber
            val watchedCount = watchedEpisodes.count { watched ->
                watched.seasonNumber == season.seasonNumber
            }
            val completedAtEpochMillis = seasonCompletionTimestamp(
                season = season,
                watchedEpisodeDates = watchedEpisodeDates
            )
            val isSeasonWatched =
                completedAtEpochMillis != null ||
                    (
                        season.episodeCount > 0 &&
                            watchedCount >= season.episodeCount
                        )

            SeasonHeader(
                season = season,
                watchedCount = watchedCount,
                isExpanded = isExpanded,
                isSeasonWatched = isSeasonWatched,
                completedAtEpochMillis = completedAtEpochMillis,
                onClick = {
                    onSeasonClick(season.seasonNumber)
                },
                onWatchedClick = {
                    onSeasonWatchedClick(
                        season.seasonNumber,
                        season.episodeCount
                    )
                }
            )

            if (isExpanded) {
                when {
                    loadingSeasonNumber == season.seasonNumber -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    seasonCache[season.seasonNumber] == null -> {
                        Text(
                            text = "Tap the season again to retry loading.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    else -> {
                        seasonCache[season.seasonNumber]
                            ?.episodes
                            .orEmpty()
                            .forEach { episode ->
                                val key = WatchedEpisodeKey(
                                    seasonNumber = episode.seasonNumber,
                                    episodeNumber = episode.episodeNumber
                                )
                                val isWatched = key in watchedEpisodes

                                EpisodeRow(
                                    episode = episode,
                                    isWatched = isWatched,
                                    onClick = {
                                        onEpisodeClick(
                                            episode.seasonNumber,
                                            episode.episodeNumber
                                        )
                                    },
                                    onWatchedClick = {
                                        onWatchedChange(
                                            episode.seasonNumber,
                                            episode.episodeNumber,
                                            !isWatched
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonHeader(
    season: SeasonSummaryDto,
    watchedCount: Int,
    isExpanded: Boolean,
    isSeasonWatched: Boolean,
    completedAtEpochMillis: Long?,
    onClick: () -> Unit,
    onWatchedClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = season.name.ifBlank {
                        "Season ${season.seasonNumber}"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$watchedCount/${season.episodeCount} watched",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (completedAtEpochMillis != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Completed ${
                            formatStoredDate(completedAtEpochMillis)
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = if (isSeasonWatched) {
                            WatchedGreen
                        } else {
                            UnwatchedGrey
                        },
                        shape = CircleShape
                    )
                    .clickable(onClick = onWatchedClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = if (isSeasonWatched) {
                        Color.White
                    } else {
                        Color.Gray
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isExpanded) "⌃" else "⌄",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: EpisodeDto,
    isWatched: Boolean,
    onClick: () -> Unit,
    onWatchedClick: () -> Unit
) {
    val imageUrl = episode.stillPath?.let { path ->
        "$TMDB_EPISODE_STILL_BASE_URL$path"
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(148.dp)
                    .height(112.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = episode.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = episode.name,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "S%02d | E%02d".format(
                        episode.seasonNumber,
                        episode.episodeNumber
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = episode.name.ifBlank {
                        "Episode ${episode.episodeNumber}"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(52.dp)
                    .background(
                        color = if (isWatched) {
                            WatchedGreen
                        } else {
                            UnwatchedGrey
                        },
                        shape = CircleShape
                    )
                    .clickable(onClick = onWatchedClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = if (isWatched) Color.White else Color.Gray,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun seasonCompletionTimestamp(
    season: SeasonSummaryDto,
    watchedEpisodeDates: Map<WatchedEpisodeKey, Long>
): Long? {
    if (season.episodeCount <= 0) {
        return null
    }

    val watchedDates = (1..season.episodeCount).map { episodeNumber ->
        watchedEpisodeDates[
            WatchedEpisodeKey(
                seasonNumber = season.seasonNumber,
                episodeNumber = episodeNumber
            )
        ] ?: return null
    }

    return watchedDates.maxOrNull()
}

private fun seriesCompletionTimestamp(
    details: SeriesDetailsDto?,
    watchedEpisodeDates: Map<WatchedEpisodeKey, Long>
): Long? {
    val seasons = details
        ?.seasons
        .orEmpty()
        .filter { season ->
            season.seasonNumber > 0 && season.episodeCount > 0
        }

    if (seasons.isEmpty()) {
        return null
    }

    val completedDates = seasons.map { season ->
        seasonCompletionTimestamp(
            season = season,
            watchedEpisodeDates = watchedEpisodeDates
        ) ?: return null
    }

    return completedDates.maxOrNull()
}

private fun seriesSubtitle(details: SeriesDetailsDto?): String {
    if (details == null) {
        return "Series information"
    }

    val seasons = when (details.numberOfSeasons) {
        1 -> "1 season"
        else -> "${details.numberOfSeasons} seasons"
    }
    val genres = details.genres
        .map { genre -> genre.name }
        .filter { name -> name.isNotBlank() }
        .joinToString(", ")

    return if (genres.isBlank()) {
        seasons
    } else {
        "$seasons • $genres"
    }
}
