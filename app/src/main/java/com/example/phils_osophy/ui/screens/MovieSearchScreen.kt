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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phils_osophy.data.local.SavedMovieEntity
import com.example.phils_osophy.data.remote.MovieDto
import com.example.phils_osophy.data.remote.TmdbClient
import kotlinx.coroutines.launch

private const val TMDB_POSTER_BASE_URL =
    "https://image.tmdb.org/t/p/w342"

private val FavoriteColor = Color(0xFFE53935)

@Suppress("UNUSED_PARAMETER")
@Composable
fun MovieSearchScreen(
    savedMovies: List<SavedMovieEntity>,
    onAddMovie: (MovieDto) -> Unit,
    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    onChangeRating: (movieId: Int, rating: Int) -> Unit,
    onRemoveMovie: (movieId: Int) -> Unit,
    onBackClick: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var movies by remember {
        mutableStateOf<List<MovieDto>>(emptyList())
    }
    var isLoading by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val savedMovieIds = savedMovies
        .map { movie -> movie.id }
        .toSet()
    val favoriteMovies = savedMovies.filter { movie ->
        movie.isFavorite
    }
    val visibleSavedMovies = when {
        showFavoritesOnly && hasSearched -> {
            favoriteMovies.filter { movie ->
                movie.title.contains(
                    other = query.trim(),
                    ignoreCase = true
                )
            }
        }

        showFavoritesOnly -> favoriteMovies
        else -> savedMovies
    }

    val coroutineScope = rememberCoroutineScope()

    val resetSearch = {
        hasSearched = false
        movies = emptyList()
        errorMessage = null
        isLoading = false
    }

    val searchMovies = {
        val cleanQuery = query.trim()

        if (cleanQuery.isNotEmpty() && !isLoading) {
            errorMessage = null
            hasSearched = true

            if (showFavoritesOnly) {
                movies = emptyList()
            } else {
                coroutineScope.launch {
                    isLoading = true

                    try {
                        movies = TmdbClient.api
                            .searchMovies(cleanQuery)
                            .results
                    } catch (exception: Exception) {
                        movies = emptyList()
                        errorMessage =
                            exception.localizedMessage
                                ?: "Movie search failed."
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 66.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Movies",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium
            )

            TextButton(
                onClick = {
                    showFavoritesOnly = !showFavoritesOnly
                    query = ""
                    resetSearch()
                }
            ) {
                Text(
                    text = if (showFavoritesOnly) "♥" else "♡",
                    color = if (showFavoritesOnly) {
                        FavoriteColor
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 30.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { newQuery ->
                    query = newQuery

                    if (newQuery.isBlank()) {
                        resetSearch()
                    }
                },
                modifier = Modifier.weight(1f),
                label = {
                    Text(
                        if (showFavoritesOnly) {
                            "Search within favorites"
                        } else {
                            "Search for a movie"
                        }
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        searchMovies()
                    }
                )
            )

            Button(
                onClick = searchMovies,
                enabled = query.isNotBlank() && !isLoading
            ) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showFavoritesOnly || !hasSearched) {
            if (showFavoritesOnly) {
                Text(
                    text = "My favorites",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            SavedMovieGrid(
                movies = visibleSavedMovies,
                onMovieClick = onMovieClick,
                onFavoriteClick = onFavoriteClick,
                onChangeRating = onChangeRating,
                onRemoveMovie = onRemoveMovie,
                emptyMessage = when {
                    showFavoritesOnly && hasSearched ->
                        "No favorite movies found."

                    showFavoritesOnly ->
                        "No favorite movies yet."

                    else ->
                        "No movies added yet."
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    movies.isEmpty() -> {
                        Text("No movies found.")
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = movies,
                                key = { movie -> movie.id }
                            ) { movie ->
                                MovieResultCard(
                                    movie = movie,
                                    isAdded =
                                        movie.id in savedMovieIds,
                                    onAddClick = {
                                        onAddMovie(movie)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieResultCard(
    movie: MovieDto,
    isAdded: Boolean,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            MoviePoster(
                movie = movie,
                modifier = Modifier.size(
                    width = 96.dp,
                    height = 144.dp
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium
                )

                if (movie.releaseDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Released: ${movie.releaseDate}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Rating: %.1f / 10".format(
                        movie.voteAverage
                    ),
                    style = MaterialTheme.typography.bodySmall
                )

                if (movie.overview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = movie.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onAddClick,
                    enabled = !isAdded
                ) {
                    Text(
                        if (isAdded) {
                            "Added"
                        } else {
                            "+ Add"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MoviePoster(
    movie: MovieDto,
    modifier: Modifier = Modifier
) {
    val posterUrl = movie.posterPath
        ?.takeIf { it.isNotBlank() }
        ?.let { posterPath ->
            "$TMDB_POSTER_BASE_URL$posterPath"
        }

    if (posterUrl == null) {
        PosterPlaceholder(
            text = "No poster",
            modifier = modifier
        )
        return
    }

    var isLoading by remember(posterUrl) {
        mutableStateOf(true)
    }
    var hasError by remember(posterUrl) {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceVariant
        ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(
                LocalContext.current
            )
                .data(posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = "${movie.title} poster",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onLoading = {
                isLoading = true
                hasError = false
            },
            onSuccess = {
                isLoading = false
                hasError = false
            },
            onError = {
                isLoading = false
                hasError = true
            }
        )

        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }

            hasError -> {
                Text(
                    text = "Poster unavailable",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PosterPlaceholder(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceVariant
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
