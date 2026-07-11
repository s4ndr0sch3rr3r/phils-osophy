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
import com.example.phils_osophy.ui.screens.BooksMenuScreen
import com.example.phils_osophy.ui.screens.EmptyPageScreen
import com.example.phils_osophy.ui.screens.MainMenuScreen
import com.example.phils_osophy.ui.screens.MovieListScreen
import com.example.phils_osophy.ui.screens.MovieSearchScreen
import com.example.phils_osophy.ui.screens.SeriesMenuScreen
import kotlinx.coroutines.launch

@Composable
fun App() {
    var currentScreen by remember {
        mutableStateOf(AppScreen.MainMenu)
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
                    coroutineScope.launch {
                        savedMovieDao.insert(
                            movie.toSavedMovieEntity()
                        )
                    }
                },
                onOpenList = {
                    currentScreen = AppScreen.MoviesList
                },
                onBackClick = ::goBackToMainMenu
            )
        }

        AppScreen.MoviesList -> {
            MovieListScreen(
                movies = savedMovies,
                onBackClick = {
                    currentScreen = AppScreen.MoviesMenu
                }
            )
        }

        AppScreen.GamesMenu -> {
            EmptyPageScreen(
                title = "Games",
                onBackClick = ::goBackToMainMenu
            )
        }

        AppScreen.SeriesMenu -> {
            SeriesMenuScreen(
                onBackClick = ::goBackToMainMenu,
                onInProgressClick = {
                    currentScreen =
                        AppScreen.SeriesInProgress
                },
                onFinishedClick = {
                    currentScreen =
                        AppScreen.SeriesFinished
                },
                onToWatchClick = {
                    currentScreen =
                        AppScreen.SeriesToWatch
                },
                onStoppedClick = {
                    currentScreen =
                        AppScreen.SeriesStopped
                }
            )
        }

        AppScreen.BooksMenu -> {
            BooksMenuScreen(
                onBackClick = ::goBackToMainMenu,
                onInProgressClick = {
                    currentScreen =
                        AppScreen.BooksInProgress
                },
                onFinishedClick = {
                    currentScreen =
                        AppScreen.BooksFinished
                },
                onToReadClick = {
                    currentScreen =
                        AppScreen.BooksToRead
                },
                onAbandonedClick = {
                    currentScreen =
                        AppScreen.BooksAbandoned
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
