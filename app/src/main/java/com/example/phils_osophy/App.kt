package com.example.phils_osophy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.phils_osophy.data.local.PhilsOsophyDatabase
import com.example.phils_osophy.data.local.WatchedEpisodeEntity
import com.example.phils_osophy.data.local.WatchedEpisodeKey
import com.example.phils_osophy.data.local.toSavedMovieEntity
import com.example.phils_osophy.data.local.toSavedSeriesEntity
import com.example.phils_osophy.data.remote.MovieDto
import com.example.phils_osophy.ui.components.AppScaffold
import com.example.phils_osophy.ui.components.BottomCategory
import com.example.phils_osophy.ui.screens.AddMovieDialog
import com.example.phils_osophy.ui.screens.BooksMenuScreen
import com.example.phils_osophy.ui.screens.EmptyPageScreen
import com.example.phils_osophy.ui.screens.EpisodeDetailScreen
import com.example.phils_osophy.ui.screens.MainMenuScreen
import com.example.phils_osophy.ui.screens.MovieDetailScreen
import com.example.phils_osophy.ui.screens.MovieListScreen
import com.example.phils_osophy.ui.screens.MovieSearchScreen
import com.example.phils_osophy.ui.screens.SeriesDetailScreen
import com.example.phils_osophy.ui.screens.SeriesLibraryScreen
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun App() {
    var currentScreen by remember {
        mutableStateOf(AppScreen.MainMenu)
    }
    var selectedMovieId by remember {
        mutableStateOf<Int?>(null)
    }
    var selectedSeriesId by remember {
        mutableStateOf<Int?>(null)
    }
    var selectedSeasonNumber by remember {
        mutableStateOf<Int?>(null)
    }
    var selectedEpisodeNumber by remember {
        mutableStateOf<Int?>(null)
    }
    var pendingMovie by remember {
        mutableStateOf<MovieDto?>(null)
    }

    val applicationContext = LocalContext.current.applicationContext
    val database = remember(applicationContext) {
        PhilsOsophyDatabase.getInstance(applicationContext)
    }
    val savedMovieDao = remember(database) {
        database.savedMovieDao()
    }
    val savedSeriesDao = remember(database) {
        database.savedSeriesDao()
    }
    val watchedEpisodeDao = remember(database) {
        database.watchedEpisodeDao()
    }

    val savedMoviesFlow = remember(savedMovieDao) {
        savedMovieDao.observeAll()
    }
    val savedMovieEntities by savedMoviesFlow.collectAsState(
        initial = emptyList()
    )

    val inProgressSeriesFlow = remember(savedSeriesDao) {
        savedSeriesDao.observeInProgress()
    }
    val inProgressSeries by inProgressSeriesFlow.collectAsState(
        initial = emptyList()
    )

    val finishedSeriesFlow = remember(savedSeriesDao) {
        savedSeriesDao.observeFinished()
    }
    val finishedSeries by finishedSeriesFlow.collectAsState(
        initial = emptyList()
    )

    val toWatchSeriesFlow = remember(savedSeriesDao) {
        savedSeriesDao.observeToWatch()
    }
    val toWatchSeries by toWatchSeriesFlow.collectAsState(
        initial = emptyList()
    )

    val stoppedSeriesFlow = remember(savedSeriesDao) {
        savedSeriesDao.observeStopped()
    }
    val stoppedSeries by stoppedSeriesFlow.collectAsState(
        initial = emptyList()
    )

    val allSavedSeries = remember(
        inProgressSeries,
        finishedSeries,
        toWatchSeries,
        stoppedSeries
    ) {
        (
            inProgressSeries +
                finishedSeries +
                toWatchSeries +
                stoppedSeries
            ).distinctBy { series -> series.id }
    }

    val watchedEpisodesFlow = remember(
        watchedEpisodeDao,
        selectedSeriesId
    ) {
        selectedSeriesId?.let { seriesId ->
            watchedEpisodeDao.observeForSeries(seriesId)
        } ?: flowOf<List<WatchedEpisodeEntity>>(emptyList())
    }
    val watchedEpisodeEntities by watchedEpisodesFlow.collectAsState(
        initial = emptyList()
    )
    val watchedEpisodeKeys = watchedEpisodeEntities
        .map { episode ->
            WatchedEpisodeKey(
                seasonNumber = episode.seasonNumber,
                episodeNumber = episode.episodeNumber
            )
        }
        .toSet()

    val coroutineScope = rememberCoroutineScope()

    fun goBackToMainMenu() {
        currentScreen = AppScreen.MainMenu
    }

    fun goBackToBooksMenu() {
        currentScreen = AppScreen.BooksMenu
    }

    fun clearSelectedSeries() {
        selectedSeriesId = null
        selectedSeasonNumber = null
        selectedEpisodeNumber = null
    }

    AppScaffold(
        selectedCategory = currentScreen.toBottomCategory(),
        onCategoryClick = { category ->
            pendingMovie = null
            selectedMovieId = null
            clearSelectedSeries()
            currentScreen = category.toAppScreen()
        }
    ) {
        when (currentScreen) {
            AppScreen.MainMenu -> {
                MainMenuScreen(
                    onMoviesClick = {
                        currentScreen = AppScreen.MoviesMenu
                    },
                    onSeriesClick = {
                        currentScreen = AppScreen.SeriesMenu
                    },
                    onGamesClick = {
                        currentScreen = AppScreen.GamesMenu
                    },
                    onBooksClick = {
                        currentScreen = AppScreen.BooksMenu
                    }
                )
            }

            AppScreen.MoviesMenu -> {
                MovieSearchScreen(
                    savedMovies = savedMovieEntities,
                    onAddMovie = { movie ->
                        pendingMovie = movie
                    },
                    onMovieClick = { movieId ->
                        selectedMovieId = movieId
                        currentScreen = AppScreen.MovieDetail
                    },
                    onFavoriteClick = { movieId, isFavorite ->
                        coroutineScope.launch {
                            savedMovieDao.updateFavorite(
                                movieId = movieId,
                                isFavorite = isFavorite
                            )
                        }
                    },
                    onBackClick = ::goBackToMainMenu
                )
            }

            AppScreen.MoviesList -> {
                MovieListScreen(
                    movies = savedMovieEntities,
                    onMovieClick = { movieId ->
                        selectedMovieId = movieId
                        currentScreen = AppScreen.MovieDetail
                    },
                    onFavoriteClick = { movieId, isFavorite ->
                        coroutineScope.launch {
                            savedMovieDao.updateFavorite(
                                movieId = movieId,
                                isFavorite = isFavorite
                            )
                        }
                    },
                    onBackClick = {
                        currentScreen = AppScreen.MoviesMenu
                    }
                )
            }

            AppScreen.MovieDetail -> {
                val selectedMovie = savedMovieEntities
                    .firstOrNull { movie ->
                        movie.id == selectedMovieId
                    }

                if (selectedMovie != null) {
                    MovieDetailScreen(
                        movie = selectedMovie,
                        onBackClick = {
                            currentScreen = AppScreen.MoviesMenu
                        },
                        onFavoriteClick = { isFavorite ->
                            coroutineScope.launch {
                                savedMovieDao.updateFavorite(
                                    movieId = selectedMovie.id,
                                    isFavorite = isFavorite
                                )
                            }
                        },
                        onChangeRating = { rating ->
                            coroutineScope.launch {
                                savedMovieDao.updateRating(
                                    movieId = selectedMovie.id,
                                    userRating = rating.coerceIn(0, 10)
                                )
                            }
                        },
                        onRemoveMovieClick = {
                            coroutineScope.launch {
                                savedMovieDao.deleteById(selectedMovie.id)
                                selectedMovieId = null
                                currentScreen = AppScreen.MoviesMenu
                            }
                        }
                    )
                } else {
                    EmptyPageScreen(
                        title = "Movie unavailable",
                        onBackClick = {
                            currentScreen = AppScreen.MoviesMenu
                        }
                    )
                }
            }

            AppScreen.SeriesMenu -> {
                SeriesLibraryScreen(
                    inProgressSeries = inProgressSeries,
                    finishedSeries = finishedSeries,
                    toWatchSeries = toWatchSeries,
                    stoppedSeries = stoppedSeries,
                    onAddSeries = { series, status ->
                        coroutineScope.launch {
                            savedSeriesDao.insert(
                                series.toSavedSeriesEntity(status = status)
                            )
                        }
                    },
                    onSeriesClick = { seriesId ->
                        selectedSeriesId = seriesId
                        selectedSeasonNumber = null
                        selectedEpisodeNumber = null
                        currentScreen = AppScreen.SeriesDetail
                    },
                    onStatusChange = { seriesId, status ->
                        coroutineScope.launch {
                            savedSeriesDao.updateStatus(
                                seriesId = seriesId,
                                status = status.name
                            )
                        }
                    },
                    onFavoriteClick = { seriesId, isFavorite ->
                        coroutineScope.launch {
                            savedSeriesDao.updateFavorite(
                                seriesId = seriesId,
                                isFavorite = isFavorite
                            )
                        }
                    },
                    onRemoveSeries = { seriesId ->
                        coroutineScope.launch {
                            watchedEpisodeDao.deleteForSeries(seriesId)
                            savedSeriesDao.deleteById(seriesId)
                        }
                    },
                    onBackClick = ::goBackToMainMenu
                )
            }

            AppScreen.SeriesDetail -> {
                val selectedSeries = allSavedSeries
                    .firstOrNull { series ->
                        series.id == selectedSeriesId
                    }

                if (selectedSeries != null) {
                    SeriesDetailScreen(
                        series = selectedSeries,
                        watchedEpisodes = watchedEpisodeKeys,
                        onBackClick = {
                            currentScreen = AppScreen.SeriesMenu
                        },
                        onFavoriteClick = { isFavorite ->
                            coroutineScope.launch {
                                savedSeriesDao.updateFavorite(
                                    seriesId = selectedSeries.id,
                                    isFavorite = isFavorite
                                )
                            }
                        },
                        onEpisodeClick = { seasonNumber, episodeNumber ->
                            selectedSeasonNumber = seasonNumber
                            selectedEpisodeNumber = episodeNumber
                            currentScreen = AppScreen.EpisodeDetail
                        },
                        onWatchedChange = {
                                seasonNumber,
                                episodeNumber,
                                watched ->
                            coroutineScope.launch {
                                if (watched) {
                                    watchedEpisodeDao.markWatched(
                                        WatchedEpisodeEntity(
                                            seriesId = selectedSeries.id,
                                            seasonNumber = seasonNumber,
                                            episodeNumber = episodeNumber
                                        )
                                    )
                                } else {
                                    watchedEpisodeDao.markUnwatched(
                                        seriesId = selectedSeries.id,
                                        seasonNumber = seasonNumber,
                                        episodeNumber = episodeNumber
                                    )
                                }
                            }
                        }
                    )
                } else {
                    EmptyPageScreen(
                        title = "Series unavailable",
                        onBackClick = {
                            currentScreen = AppScreen.SeriesMenu
                        }
                    )
                }
            }

            AppScreen.EpisodeDetail -> {
                val selectedSeries = allSavedSeries
                    .firstOrNull { series ->
                        series.id == selectedSeriesId
                    }
                val seasonNumber = selectedSeasonNumber
                val episodeNumber = selectedEpisodeNumber

                if (
                    selectedSeries != null &&
                    seasonNumber != null &&
                    episodeNumber != null
                ) {
                    val episodeKey = WatchedEpisodeKey(
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber
                    )

                    EpisodeDetailScreen(
                        seriesId = selectedSeries.id,
                        seriesName = selectedSeries.name,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        isWatched = episodeKey in watchedEpisodeKeys,
                        onBackClick = {
                            currentScreen = AppScreen.SeriesDetail
                        },
                        onWatchedChange = { watched ->
                            coroutineScope.launch {
                                if (watched) {
                                    watchedEpisodeDao.markWatched(
                                        WatchedEpisodeEntity(
                                            seriesId = selectedSeries.id,
                                            seasonNumber = seasonNumber,
                                            episodeNumber = episodeNumber
                                        )
                                    )
                                } else {
                                    watchedEpisodeDao.markUnwatched(
                                        seriesId = selectedSeries.id,
                                        seasonNumber = seasonNumber,
                                        episodeNumber = episodeNumber
                                    )
                                }
                            }
                        }
                    )
                } else {
                    EmptyPageScreen(
                        title = "Episode unavailable",
                        onBackClick = {
                            currentScreen = AppScreen.SeriesMenu
                        }
                    )
                }
            }

            AppScreen.Explore -> {
                EmptyPageScreen(
                    title = "Explore",
                    onBackClick = ::goBackToMainMenu
                )
            }

            AppScreen.Profile -> {
                EmptyPageScreen(
                    title = "Profile",
                    onBackClick = ::goBackToMainMenu
                )
            }

            AppScreen.GamesMenu -> {
                EmptyPageScreen(
                    title = "Games",
                    onBackClick = ::goBackToMainMenu
                )
            }

            AppScreen.BooksMenu -> {
                BooksMenuScreen(
                    onBackClick = ::goBackToMainMenu,
                    onInProgressClick = {
                        currentScreen = AppScreen.BooksInProgress
                    },
                    onFinishedClick = {
                        currentScreen = AppScreen.BooksFinished
                    },
                    onToReadClick = {
                        currentScreen = AppScreen.BooksToRead
                    },
                    onAbandonedClick = {
                        currentScreen = AppScreen.BooksAbandoned
                    }
                )
            }

            AppScreen.BooksInProgress -> {
                EmptyPageScreen(
                    title = "Livres en cours",
                    onBackClick = ::goBackToBooksMenu
                )
            }

            AppScreen.BooksFinished -> {
                EmptyPageScreen(
                    title = "Livres terminés",
                    onBackClick = ::goBackToBooksMenu
                )
            }

            AppScreen.BooksToRead -> {
                EmptyPageScreen(
                    title = "Livres à lire",
                    onBackClick = ::goBackToBooksMenu
                )
            }

            AppScreen.BooksAbandoned -> {
                EmptyPageScreen(
                    title = "Livres abandonnés",
                    onBackClick = ::goBackToBooksMenu
                )
            }
        }
    }

    pendingMovie?.let { movie ->
        AddMovieDialog(
            movie = movie,
            onAdd = { rating, note ->
                coroutineScope.launch {
                    savedMovieDao.insert(
                        movie.toSavedMovieEntity(
                            userRating = rating,
                            note = note
                        )
                    )
                }
                pendingMovie = null
            },
            onCancel = {
                pendingMovie = null
            }
        )
    }
}

private fun AppScreen.toBottomCategory(): BottomCategory? = when (this) {
    AppScreen.MoviesMenu,
    AppScreen.MoviesList,
    AppScreen.MovieDetail -> BottomCategory.Movies

    AppScreen.SeriesMenu,
    AppScreen.SeriesDetail,
    AppScreen.EpisodeDetail -> BottomCategory.Series

    AppScreen.Explore -> BottomCategory.Explore
    AppScreen.Profile -> BottomCategory.Profile
    AppScreen.GamesMenu -> BottomCategory.Games

    AppScreen.BooksMenu,
    AppScreen.BooksInProgress,
    AppScreen.BooksFinished,
    AppScreen.BooksToRead,
    AppScreen.BooksAbandoned -> BottomCategory.Books

    AppScreen.MainMenu -> null
}

private fun BottomCategory.toAppScreen(): AppScreen = when (this) {
    BottomCategory.Movies -> AppScreen.MoviesMenu
    BottomCategory.Series -> AppScreen.SeriesMenu
    BottomCategory.Explore -> AppScreen.Explore
    BottomCategory.Profile -> AppScreen.Profile
    BottomCategory.Games -> AppScreen.GamesMenu
    BottomCategory.Books -> AppScreen.BooksMenu
}
