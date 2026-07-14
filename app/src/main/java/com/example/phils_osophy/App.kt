package com.example.phils_osophy

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.phils_osophy.data.local.PhilsOsophyDatabase
import com.example.phils_osophy.data.local.RecipeStatus
import com.example.phils_osophy.data.local.SavedGameEntity
import com.example.phils_osophy.data.local.createSavedRecipeEntity
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
import com.example.phils_osophy.ui.screens.GameLibraryScreen
import com.example.phils_osophy.ui.screens.MovieDetailScreen
import com.example.phils_osophy.ui.screens.MovieListScreen
import com.example.phils_osophy.ui.screens.MovieSearchScreen
import com.example.phils_osophy.ui.screens.ProfileScreen
import com.example.phils_osophy.ui.screens.RecipeDetailScreen
import com.example.phils_osophy.ui.screens.RecipeLibraryScreen
import com.example.phils_osophy.ui.screens.SeriesDetailScreen
import com.example.phils_osophy.ui.screens.SeriesLibraryScreen
import com.example.phils_osophy.ui.screens.USER_RATING_MAX
import kotlinx.coroutines.launch

@Composable
fun App() {
    var currentScreen by remember {
        mutableStateOf(AppScreen.MoviesMenu)
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
    var selectedRecipeKey by remember {
        mutableStateOf<String?>(null)
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
    val savedGameDao = remember(database) {
        database.savedGameDao()
    }
    val savedBookDao = remember(database) {
        database.savedBookDao()
    }
    val savedRecipeDao = remember(database) {
        database.savedRecipeDao()
    }

    val savedMoviesFlow = remember(savedMovieDao) {
        savedMovieDao.observeAll()
    }
    val savedMovieEntities by savedMoviesFlow.collectAsState(
        initial = emptyList()
    )

    val savedGamesFlow = remember(savedGameDao) {
        savedGameDao.observeAll()
    }
    val savedGames by savedGamesFlow.collectAsState(
        initial = emptyList()
    )

    val savedBooksFlow = remember(savedBookDao) {
        savedBookDao.observeAll()
    }
    val savedBooks by savedBooksFlow.collectAsState(
        initial = emptyList()
    )

    val inProgressRecipesFlow = remember(savedRecipeDao) {
        savedRecipeDao.observeInProgress()
    }
    val inProgressRecipes by inProgressRecipesFlow.collectAsState(
        initial = emptyList()
    )

    val finishedRecipesFlow = remember(savedRecipeDao) {
        savedRecipeDao.observeFinished()
    }
    val finishedRecipes by finishedRecipesFlow.collectAsState(
        initial = emptyList()
    )

    val toTryRecipesFlow = remember(savedRecipeDao) {
        savedRecipeDao.observeToTry()
    }
    val toTryRecipes by toTryRecipesFlow.collectAsState(
        initial = emptyList()
    )

    val stoppedRecipesFlow = remember(savedRecipeDao) {
        savedRecipeDao.observeStopped()
    }
    val stoppedRecipes by stoppedRecipesFlow.collectAsState(
        initial = emptyList()
    )

    val allSavedRecipes = remember(
        inProgressRecipes,
        finishedRecipes,
        toTryRecipes,
        stoppedRecipes
    ) {
        (
            inProgressRecipes +
                finishedRecipes +
                toTryRecipes +
                stoppedRecipes
            ).distinctBy { recipe -> recipe.key }
    }

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

    val allWatchedEpisodesFlow = remember(watchedEpisodeDao) {
        watchedEpisodeDao.observeAll()
    }
    val allWatchedEpisodes by allWatchedEpisodesFlow.collectAsState(
        initial = emptyList()
    )
    val watchedEpisodeEntities = remember(
        allWatchedEpisodes,
        selectedSeriesId
    ) {
        selectedSeriesId?.let { seriesId ->
            allWatchedEpisodes.filter { episode ->
                episode.seriesId == seriesId
            }
        }.orEmpty()
    }
    val watchedEpisodeKeys = watchedEpisodeEntities
        .map { episode ->
            WatchedEpisodeKey(
                seasonNumber = episode.seasonNumber,
                episodeNumber = episode.episodeNumber
            )
        }
        .toSet()

    val coroutineScope = rememberCoroutineScope()

    fun clearSelectedSeries() {
        selectedSeriesId = null
        selectedSeasonNumber = null
        selectedEpisodeNumber = null
    }

    fun clearSelectedRecipe() {
        selectedRecipeKey = null
    }

    fun openRecipes() {
        clearSelectedRecipe()
        currentScreen = AppScreen.RecipesMenu
    }

    fun openMovies() {
        pendingMovie = null
        selectedMovieId = null
        clearSelectedSeries()
        clearSelectedRecipe()
        currentScreen = AppScreen.MoviesMenu
    }

    fun openSeriesLibrary() {
        selectedSeasonNumber = null
        selectedEpisodeNumber = null
        currentScreen = AppScreen.SeriesMenu
    }

    fun openProfile() {
        pendingMovie = null
        selectedMovieId = null
        clearSelectedSeries()
        clearSelectedRecipe()
        currentScreen = AppScreen.Profile
    }

    BackHandler(enabled = currentScreen != AppScreen.MoviesMenu) {
        when (currentScreen) {
            AppScreen.MoviesList,
            AppScreen.MovieDetail -> openMovies()

            AppScreen.SeriesMenu -> openMovies()
            AppScreen.SeriesDetail -> openSeriesLibrary()
            AppScreen.EpisodeDetail -> currentScreen = AppScreen.SeriesDetail

            AppScreen.Explore,
            AppScreen.Profile,
            AppScreen.GamesMenu,
            AppScreen.BooksMenu,
            AppScreen.RecipesMenu -> openMovies()

            AppScreen.RecipeDetail -> openRecipes()
            AppScreen.MoviesMenu -> Unit
        }
    }

    AppScaffold(
        selectedCategory = currentScreen.toBottomCategory(),
        onCategoryClick = { category ->
            pendingMovie = null
            selectedMovieId = null
            clearSelectedSeries()
            clearSelectedRecipe()
            currentScreen = category.toAppScreen()
        },
        isProfileSelected = currentScreen == AppScreen.Profile,
        onProfileClick = ::openProfile
    ) {
        when (currentScreen) {
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
                    onBackClick = ::openMovies
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
                    onBackClick = ::openMovies
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
                        onBackClick = ::openMovies,
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
                                    userRating = rating.coerceIn(0, USER_RATING_MAX)
                                )
                            }
                        },
                        onRemoveMovieClick = {
                            coroutineScope.launch {
                                savedMovieDao.deleteById(selectedMovie.id)
                                openMovies()
                            }
                        }
                    )
                } else {
                    EmptyPageScreen(
                        title = "Movie unavailable",
                        onBackClick = ::openMovies
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
                    onFavoriteClick = { seriesId, isFavorite ->
                        coroutineScope.launch {
                            savedSeriesDao.updateFavorite(
                                seriesId = seriesId,
                                isFavorite = isFavorite
                            )
                        }
                    },
                    onBackClick = ::openMovies
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
                        onBackClick = ::openSeriesLibrary,
                        onFavoriteClick = { isFavorite ->
                            coroutineScope.launch {
                                savedSeriesDao.updateFavorite(
                                    seriesId = selectedSeries.id,
                                    isFavorite = isFavorite
                                )
                            }
                        },
                        onStatusChange = { status ->
                            coroutineScope.launch {
                                savedSeriesDao.updateStatus(
                                    seriesId = selectedSeries.id,
                                    status = status.name
                                )
                            }
                        },
                        onRemoveSeries = {
                            coroutineScope.launch {
                                watchedEpisodeDao.deleteForSeries(selectedSeries.id)
                                savedSeriesDao.deleteById(selectedSeries.id)
                                openSeriesLibrary()
                            }
                        },
                        onChangeRating = { rating ->
                            coroutineScope.launch {
                                savedSeriesDao.updateRating(
                                    seriesId = selectedSeries.id,
                                    userRating = rating.coerceIn(0, USER_RATING_MAX)
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
                        onBackClick = ::openSeriesLibrary
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
                        onBackClick = ::openSeriesLibrary
                    )
                }
            }

            AppScreen.Explore -> {
                EmptyPageScreen(
                    title = "Explore",
                    onBackClick = ::openMovies
                )
            }

            AppScreen.Profile -> {
                ProfileScreen(
                    movies = savedMovieEntities,
                    series = allSavedSeries,
                    watchedEpisodes = allWatchedEpisodes,
                    games = savedGames,
                    books = savedBooks,
                    onBackClick = ::openMovies
                )
            }

            AppScreen.GamesMenu -> {
                GameLibraryScreen(
                    games = savedGames,
                    onAddGame = {
                            name,
                            hoursPlayedMinutes,
                            playerCount,
                            isFinished ->
                        coroutineScope.launch {
                            savedGameDao.insert(
                                SavedGameEntity(
                                    name = name,
                                    hoursPlayedMinutes = hoursPlayedMinutes,
                                    playerCount = playerCount,
                                    isFinished = isFinished
                                )
                            )
                        }
                    },
                    onChangeRating = { gameId, rating ->
                        coroutineScope.launch {
                            savedGameDao.updateRating(
                                gameId = gameId,
                                userRating = rating.coerceIn(0, USER_RATING_MAX)
                            )
                        }
                    },
                    onBackClick = ::openMovies
                )
            }

            AppScreen.BooksMenu -> {
                BooksMenuScreen(
                    onBackClick = ::openMovies,
                    onInProgressClick = {},
                    onFinishedClick = {},
                    onToReadClick = {},
                    onAbandonedClick = {}
                )
            }

            AppScreen.RecipesMenu -> {
                RecipeLibraryScreen(
                    inProgressRecipes = inProgressRecipes,
                    finishedRecipes = finishedRecipes,
                    toTryRecipes = toTryRecipes,
                    stoppedRecipes = stoppedRecipes,
                    onAddRecipe = { title, status ->
                        coroutineScope.launch {
                            savedRecipeDao.insert(
                                createSavedRecipeEntity(
                                    title = title,
                                    status = status
                                )
                            )
                        }
                    },
                    onRecipeClick = { recipeKey ->
                        selectedRecipeKey = recipeKey
                        currentScreen = AppScreen.RecipeDetail
                    },
                    onFavoriteClick = { recipeKey, isFavorite ->
                        coroutineScope.launch {
                            savedRecipeDao.updateFavorite(
                                recipeKey = recipeKey,
                                isFavorite = isFavorite
                            )
                        }
                    },
                    onBackClick = ::openMovies
                )
            }

            AppScreen.RecipeDetail -> {
                val selectedRecipe = allSavedRecipes.firstOrNull { recipe ->
                    recipe.key == selectedRecipeKey
                }

                if (selectedRecipe != null) {
                    RecipeDetailScreen(
                        recipe = selectedRecipe,
                        onBackClick = ::openRecipes,
                        onFavoriteClick = { isFavorite ->
                            coroutineScope.launch {
                                savedRecipeDao.updateFavorite(
                                    recipeKey = selectedRecipe.key,
                                    isFavorite = isFavorite
                                )
                            }
                        },
                        onStatusChange = { status ->
                            coroutineScope.launch {
                                savedRecipeDao.updateStatus(
                                    recipeKey = selectedRecipe.key,
                                    status = status.name
                                )
                            }
                        },
                        onSaveDetails = {
                                difficulty,
                                prepTimeMinutes,
                                totalTimeMinutes,
                                ingredients,
                                cookingSteps ->
                            coroutineScope.launch {
                                savedRecipeDao.updateDetails(
                                    recipeKey = selectedRecipe.key,
                                    difficulty = difficulty,
                                    prepTimeMinutes = prepTimeMinutes,
                                    totalTimeMinutes = totalTimeMinutes,
                                    ingredients = ingredients,
                                    cookingSteps = cookingSteps
                                )
                            }
                        },
                        onRemoveRecipe = {
                            coroutineScope.launch {
                                savedRecipeDao.deleteByKey(selectedRecipe.key)
                                openRecipes()
                            }
                        }
                    )
                } else {
                    EmptyPageScreen(
                        title = "Recipe unavailable",
                        onBackClick = ::openRecipes
                    )
                }
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

    AppScreen.Explore,
    AppScreen.Profile -> null
    AppScreen.GamesMenu -> BottomCategory.Games
    AppScreen.BooksMenu -> BottomCategory.Books
    AppScreen.RecipesMenu,
    AppScreen.RecipeDetail -> BottomCategory.Recipes
}

private fun BottomCategory.toAppScreen(): AppScreen = when (this) {
    BottomCategory.Series -> AppScreen.SeriesMenu
    BottomCategory.Movies -> AppScreen.MoviesMenu
    BottomCategory.Books -> AppScreen.BooksMenu
    BottomCategory.Recipes -> AppScreen.RecipesMenu
    BottomCategory.Games -> AppScreen.GamesMenu
}
