package com.example.phils_osophy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phils_osophy.data.local.SavedMovieEntity

private const val TMDB_POSTER_BASE_URL =
    "https://image.tmdb.org/t/p/w342"

private val FavoriteColor = Color(0xFFE53935)

@Composable
fun MovieListScreen(
    movies: List<SavedMovieEntity>,
    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    onChangeRating: (movieId: Int, rating: Int) -> Unit,
    onRemoveMovie: (movieId: Int) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 8.dp)
    ) {
        TextButton(onClick = onBackClick) {
            Text("← Back")
        }

        Text(
            text = "My movie list",
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        SavedMovieGrid(
            movies = movies,
            onMovieClick = onMovieClick,
            onFavoriteClick = onFavoriteClick,
            onChangeRating = onChangeRating,
            onRemoveMovie = onRemoveMovie,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SavedMovieGrid(
    movies: List<SavedMovieEntity>,
    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    onChangeRating: (movieId: Int, rating: Int) -> Unit,
    onRemoveMovie: (movieId: Int) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "No movies added yet."
) {
    if (movies.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(emptyMessage)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(
            items = movies,
            key = { movie -> movie.id }
        ) { movie ->
            MoviePosterTile(
                movie = movie,
                onClick = {
                    onMovieClick(movie.id)
                },
                onFavoriteClick = {
                    onFavoriteClick(
                        movie.id,
                        !movie.isFavorite
                    )
                },
                onChangeRating = { rating ->
                    onChangeRating(movie.id, rating)
                },
                onRemoveMovie = {
                    onRemoveMovie(movie.id)
                }
            )
        }
    }
}

@Composable
private fun MoviePosterTile(
    movie: SavedMovieEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onChangeRating: (Int) -> Unit,
    onRemoveMovie: () -> Unit
) {
    val posterUrl = movie.posterPath
        ?.takeIf { path -> path.isNotBlank() }
        ?.let { path -> "$TMDB_POSTER_BASE_URL$path" }

    var isLoaded by remember(posterUrl) {
        mutableStateOf(false)
    }
    var hasError by remember(posterUrl) {
        mutableStateOf(false)
    }
    var showManageDialog by remember(movie.id) {
        mutableStateOf(false)
    }
    var showRatingDialog by remember(movie.id) {
        mutableStateOf(false)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (posterUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${movie.title} poster",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onLoading = {
                        isLoaded = false
                        hasError = false
                    },
                    onSuccess = {
                        isLoaded = true
                        hasError = false
                    },
                    onError = {
                        isLoaded = false
                        hasError = true
                    }
                )
            }

            if (posterUrl == null || !isLoaded || hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.76f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = movie.title,
                        modifier = Modifier.padding(10.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable(onClick = onFavoriteClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (movie.isFavorite) "♥" else "♡",
                    color = if (movie.isFavorite) {
                        FavoriteColor
                    } else {
                        Color.White
                    },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            UserRatingBadge(
                rating = movie.userRating,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )

            Text(
                text = "•••",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable {
                        showManageDialog = true
                    }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showManageDialog) {
        MovieManageDialog(
            movie = movie,
            onChangeRating = {
                showManageDialog = false
                showRatingDialog = true
            },
            onRemove = {
                onRemoveMovie()
                showManageDialog = false
            },
            onCancel = {
                showManageDialog = false
            }
        )
    }

    if (showRatingDialog) {
        UserRatingDialog(
            title = "Rate ${movie.title}",
            initialRating = movie.userRating,
            onSave = { rating ->
                onChangeRating(rating)
                showRatingDialog = false
            },
            onCancel = {
                showRatingDialog = false
            }
        )
    }
}

@Composable
private fun MovieManageDialog(
    movie: SavedMovieEntity,
    onChangeRating: () -> Unit,
    onRemove: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(movie.title) },
        text = {
            Column {
                Text(
                    text = if (movie.userRating in 1..10) {
                        "Your rating: ${movie.userRating} / 10"
                    } else {
                        "Not rated"
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onChangeRating) {
                    Text(
                        if (movie.userRating in 1..10) {
                            "Change rating"
                        } else {
                            "Add rating"
                        }
                    )
                }
                TextButton(onClick = onRemove) {
                    Text(
                        text = "Remove movie",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) {
                Text("Close")
            }
        }
    )
}
