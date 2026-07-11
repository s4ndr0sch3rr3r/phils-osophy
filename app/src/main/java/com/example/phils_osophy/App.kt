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
import com.example.phils_osophy.data.local.toMovieDto
import com.example.phils_osophy.data.local.toSavedMovieEntity
import com.example.phils_osophy.data.remote.MovieDto
import com.example.phils_osophy.ui.components.AppScaffold
import com.example.phils_osophy.ui.components.BottomCategory
import com.example.phils_osophy.ui.screens.AddMovieDialog
import com.example.phils_osophy.ui.screens.BooksMenuScreen
import com.example.phils_osophy.ui.screens.EmptyPageScreen
import com.example.phils_osophy.ui.screens.MainMenuScreen
import com.example.phils_osophy.ui.screens.MovieDetailScreen
import com.example.phils_osophy.ui.screens.MovieListScreen
import com.example.phils_osophy.ui.screens.MovieSearchScreen
import com.example.phils_osophy.ui.screens.SeriesMenuScreen
import kotlinx.coroutines.launch

@Composable
fun App() {
    var currentScreen by remember {
        mutableStateOf(AppScreen.MainMenu)
    }
    var selectedMovieId by remember {
        mutableStateOf<Int?>(null)
    }
    var pendingMovie by remember {
        mutableStateOf<MovieDto?>(null)
    }

    val applicationContext = LocalContext.current.applicationContext
    val savedMovieDao = remember(applicationContext) {
        PhilsOsophyDatabase
            .getInstance(applicationContext)
            .savedMovieDao()
    }
    val savedMoviesFlow = remember(savedMovieDao) {
        savedMovieDao.observeAll()
    }
    val savedMovieEntities by savedMoviesFlow.collectAsState(
        initial = emptyList()
    )
    val savedMovies = savedMovieEntities.map { entity ->
        entity.toMovieDto()
    }
    val coroutineScope = rememberCoroutineScope()

    fun goBackToMainMenu() {
        currentScreen = AppScreen.MainMenu
    }

    fun goBackToSeriesMenu() {
        currentScreen = AppScreen.SeriesMenu
    }

    fun goBackToBooksMenu() {
        currentScreen = AppScreen.BooksMenu
    }

    AppScaffold(
        selectedCategory = currentScreen.toBottomCategory(),
        onCategoryClick = { category ->
            pendingMovie = null
            selectedMovieId = null
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
                    savedMovieIds = savedMovies
                        .map { movie -> movie.id }
                        .toSet(),
                    onAddMovie = { movie ->
                        pendingMovie = movie
                    },
                    onOpenList = {
                        currentScreen = AppScreen.MoviesList
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
                            currentScreen = AppScreen.MoviesList
                        }
                    )
                } else {
                    EmptyPageScreen(
                        title = "Movie unavailable",
                        onBackClick = {
                            currentScreen = AppScreen.MoviesList
                        }
                    )
                }
            }

            AppScreen.SeriesMenu -> {
                SeriesMenuScreen(
                    onBackClick = ::goBackToMainMenu,
                    onInProgressClick = {
                        currentScreen = AppScreen.SeriesInProgress
                    },
                    onFinishedClick = {
                        currentScreen = AppScreen.SeriesFinished
                    },
                    onToWatchClick = {
                        currentScreen = AppScreen.SeriesToWatch
                    },
                    onStoppedClick = {
                        currentScreen = AppScreen.SeriesStopped
                    }
                )
            }

            AppScreen.SeriesInProgress -> {
                EmptyPageScreen(
                    title = "Séries en cours",
                    onBackClick = ::goBackToSeriesMenu
                )
            }

            AppScreen.SeriesFinished -> {
                EmptyPageScreen(
                    title = "Séries terminées",
                    onBackClick = ::goBackToSeriesMenu
                )
            }

            AppScreen.SeriesToWatch -> {
                EmptyPageScreen(
                    title = "Séries à regarder",
                    onBackClick = ::goBackToSeriesMenu
                )
            }

            AppScreen.SeriesStopped -> {
                EmptyPageScreen(
                    title = "Séries arrêtées",
                    onBackClick = ::goBackToSeriesMenu
                )
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
    AppScreen.SeriesInProgress,
    AppScreen.SeriesFinished,
    AppScreen.SeriesToWatch,
    AppScreen.SeriesStopped -> BottomCategory.Series

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
