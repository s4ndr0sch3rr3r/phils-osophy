package com.example.phils_osophy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phils_osophy.data.local.SavedMovieEntity
import com.example.phils_osophy.data.local.toMovieDto
import com.example.phils_osophy.data.remote.MovieDto
import com.example.phils_osophy.data.remote.TmdbClient
import com.example.phils_osophy.ui.components.FavoriteIcon
import kotlinx.coroutines.CancellationException

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
    onBackClick: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var remoteMovies by remember {
        mutableStateOf<List<MovieDto>>(emptyList())
    }
    var isLoading by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var isSearchBarVisible by remember { mutableStateOf(true) }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val cleanQuery = query.trim()
    val savedMovieIds = remember(savedMovies) {
        savedMovies.map { movie -> movie.id }.toSet()
    }
    val favoriteMovies = remember(savedMovies) {
        savedMovies.filter { movie -> movie.isFavorite }
    }
    val visibleSavedMovies = when {
        showFavoritesOnly && hasSearched -> {
            favoriteMovies.filter { movie ->
                movie.title.contains(
                    other = cleanQuery,
                    ignoreCase = true
                )
            }
        }

        showFavoritesOnly -> favoriteMovies
        else -> savedMovies
    }
    val localMovieResults = if (hasSearched && !showFavoritesOnly) {
        savedMovies.filter { movie ->
            movie.title.contains(
                other = cleanQuery,
                ignoreCase = true
            )
        }
    } else {
        emptyList()
    }
    val visibleRemoteMovies = remoteMovies.filterNot { movie ->
        movie.id in savedMovieIds
    }

    val searchBarScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                when {
                    available.y < -2f -> isSearchBarVisible = false
                    available.y > 2f -> isSearchBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val resetSearch = {
        hasSearched = false
        remoteMovies = emptyList()
        errorMessage = null
        isLoading = false
    }

    LaunchedEffect(query, showFavoritesOnly) {
        val submittedQuery = query.trim()

        if (submittedQuery.isBlank()) {
            resetSearch()
            return@LaunchedEffect
        }

        hasSearched = true
        errorMessage = null
        remoteMovies = emptyList()

        if (showFavoritesOnly) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        try {
            remoteMovies = TmdbClient.api
                .searchMovies(submittedQuery)
                .results
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            remoteMovies = emptyList()
            errorMessage = exception.localizedMessage
                ?: "Movie search failed."
        } finally {
            if (query.trim() == submittedQuery && !showFavoritesOnly) {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(searchBarScrollConnection)
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
                text = "Movies (${savedMovies.size})",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium
            )

            TextButton(
                onClick = {
                    showFavoritesOnly = !showFavoritesOnly
                    query = ""
                    isSearchBarVisible = true
                    resetSearch()
                }
            ) {
                FavoriteIcon(
                    isFavorite = showFavoritesOnly,
                    size = 30.dp,
                    activeColor = FavoriteColor,
                    inactiveColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = isSearchBarVisible) {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { newQuery ->
                        query = newQuery
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (showFavoritesOnly) {
                                "Search within favorites"
                            } else {
                                "Search for a movie"
                            }
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Text(
                                    text = "×",
                                    color = FavoriteColor,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (localMovieResults.isNotEmpty()) {
                    item(key = "local-movies-heading") {
                        SearchSectionHeading("In your library")
                    }
                    items(
                        items = localMovieResults,
                        key = { movie -> "local-${movie.id}" }
                    ) { movie ->
                        MovieResultCard(
                            movie = movie.toMovieDto(),
                            onImageClick = {
                                onMovieClick(movie.id)
                            }
                        )
                    }
                }

                item(key = "tmdb-movies-heading") {
                    SearchSectionHeading("TMDB results")
                }

                if (isLoading) {
                    item(key = "tmdb-movies-loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                errorMessage?.let { message ->
                    item(key = "tmdb-movies-error") {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                items(
                    items = visibleRemoteMovies,
                    key = { movie -> "remote-${movie.id}" }
                ) { movie ->
                    MovieResultCard(
                        movie = movie,
                        onAddClick = {
                            onAddMovie(movie)
                        }
                    )
                }

                if (
                    !isLoading &&
                    errorMessage == null &&
                    localMovieResults.isEmpty() &&
                    visibleRemoteMovies.isEmpty()
                ) {
                    item(key = "movies-empty") {
                        Text("No movies found.")
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun MovieResultCard(
    movie: MovieDto,
    onImageClick: (() -> Unit)? = null,
    onAddClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            MoviePoster(
                movie = movie,
                modifier = Modifier
                    .size(
                        width = 96.dp,
                        height = 144.dp
                    )
                    .then(
                        if (onImageClick != null) {
                            Modifier.clickable(onClick = onImageClick)
                        } else {
                            Modifier
                        }
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

                onAddClick?.let { addClick ->
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(onClick = addClick) {
                        Text("+ Add")
                    }
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
