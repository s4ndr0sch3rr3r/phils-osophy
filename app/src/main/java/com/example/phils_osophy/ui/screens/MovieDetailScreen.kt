package com.example.phils_osophy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phils_osophy.data.local.PhilsOsophyDatabase
import com.example.phils_osophy.data.local.SavedMovieEntity
import com.example.phils_osophy.data.remote.MovieDetailsDto
import com.example.phils_osophy.data.remote.TmdbClient
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private const val TMDB_BACKDROP_BASE_URL =
    "https://image.tmdb.org/t/p/w780"

@Composable
fun MovieDetailScreen(
    movie: SavedMovieEntity,
    onBackClick: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit,
    onChangeRating: (Int) -> Unit,
    onRemoveMovieClick: () -> Unit
) {
    var details by remember(movie.id) {
        mutableStateOf<MovieDetailsDto?>(null)
    }
    var isLoading by remember(movie.id) {
        mutableStateOf(true)
    }
    var hasError by remember(movie.id) {
        mutableStateOf(false)
    }
    var isMenuExpanded by remember {
        mutableStateOf(false)
    }
    val applicationContext = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(movie.id) {
        isLoading = true
        hasError = false

        try {
            details = TmdbClient.api.getMovieDetails(movie.id)
        } catch (_: Exception) {
            hasError = true
        } finally {
            isLoading = false
        }
    }

    val title = details?.title
        ?.takeIf { it.isNotBlank() }
        ?: movie.title
    val overview = details?.overview
        ?.takeIf { it.isNotBlank() }
        ?: movie.overview
    val releaseDate = details?.releaseDate
        ?.takeIf { it.isNotBlank() }
        ?: movie.releaseDate
    val imagePath = details?.backdropPath
        ?: details?.posterPath
        ?: movie.posterPath
    val imageUrl = imagePath?.let { path ->
        "$TMDB_BACKDROP_BASE_URL$path"
    }
    val detailLine = when {
        details != null -> {
            val runtime = formatRuntime(details?.runtime)
            val genres = details?.genres
                .orEmpty()
                .map { genre -> genre.name }
                .filter { name -> name.isNotBlank() }
                .joinToString(", ")
                .ifBlank { "Genres unavailable" }

            "$runtime • $genres"
        }

        isLoading -> "Loading runtime and genres..."
        else -> "Runtime and genres unavailable"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(
                        LocalContext.current
                    )
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
                        .background(
                            MaterialTheme.colorScheme
                                .surfaceVariant
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
                                Color.Black.copy(alpha = 0.9f)
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

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 66.dp, top = 20.dp)
            ) {
                TextButton(
                    onClick = {
                        isMenuExpanded = true
                    }
                ) {
                    Text(
                        text = "•••",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = {
                        isMenuExpanded = false
                    }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (movie.isFavorite) {
                                    "Remove favorite"
                                } else {
                                    "Favorite"
                                }
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onFavoriteClick(!movie.isFavorite)
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Remove movie",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onRemoveMovieClick()
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography
                        .headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = detailLine,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.Top
        ) {
            DateBlock(
                label = "Release date",
                value = formatReleaseDate(releaseDate),
                modifier = Modifier.weight(1f)
            )

            DateBlock(
                label = "Added",
                value = formatAddedDate(
                    movie.addedAtEpochMillis
                ),
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider()

        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (hasError) {
                Text(
                    text =
                        "Additional movie details could not be loaded.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = overview.ifBlank {
                    "No movie information available."
                },
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(20.dp))

            InlineUserRatingStars(
                rating = movie.userRating,
                onRatingChange = onChangeRating
            )
        }

        HorizontalDivider()

        EditableMediaCommentSection(
            mediaKey = movie.id,
            savedComment = movie.note,
            onSave = { savedComment ->
                coroutineScope.launch {
                    PhilsOsophyDatabase
                        .getInstance(applicationContext)
                        .savedMovieDao()
                        .updateNote(
                            movieId = movie.id,
                            note = savedComment
                        )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

}

@Composable
private fun DateBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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

private fun formatRuntime(runtimeMinutes: Int?): String {
    if (runtimeMinutes == null || runtimeMinutes <= 0) {
        return "Runtime unavailable"
    }

    val hours = runtimeMinutes / 60
    val minutes = runtimeMinutes % 60

    return when {
        hours == 0 -> "${minutes}m"
        minutes == 0 -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}

private fun formatReleaseDate(value: String): String {
    if (value.isBlank()) {
        return "Unknown"
    }

    return runCatching {
        LocalDate.parse(value).format(displayDateFormatter)
    }.getOrDefault(value)
}

private fun formatAddedDate(epochMillis: Long): String {
    if (epochMillis <= 0L) {
        return "Unknown"
    }

    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(displayDateFormatter)
}

private val displayDateFormatter =
    DateTimeFormatter.ofPattern(
        "MMMM d, yyyy",
        Locale.ENGLISH
    )
