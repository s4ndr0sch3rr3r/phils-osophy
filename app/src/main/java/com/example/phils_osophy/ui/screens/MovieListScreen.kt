package com.example.phils_osophy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phils_osophy.data.local.SavedMovieEntity

private const val TMDB_POSTER_BASE_URL =
    "https://image.tmdb.org/t/p/w342"

@Composable
fun MovieListScreen(
    movies: List<SavedMovieEntity>,
    onMovieClick: (Int) -> Unit,
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

        if (movies.isEmpty()) {
            Text(
                text = "No movies added yet.",
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement =
                    androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                verticalArrangement =
                    androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = movies,
                    key = { movie -> movie.id }
                ) { movie ->
                    MoviePosterTile(
                        movie = movie,
                        onClick = {
                            onMovieClick(movie.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MoviePosterTile(
    movie: SavedMovieEntity,
    onClick: () -> Unit
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
        }
    }
}
