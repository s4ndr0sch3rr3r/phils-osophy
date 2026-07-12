from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_exact(text: str, old: str, new: str, expected: int = 1) -> str:
    actual = text.count(old)
    if actual != expected:
        raise RuntimeError(
            f"Expected {expected} occurrence(s), found {actual}: {old[:100]!r}"
        )
    return text.replace(old, new)


# App callback wiring.
path = ROOT / "app/src/main/java/com/example/phils_osophy/App.kt"
text = path.read_text(encoding="utf-8")
movie_callbacks_old = '''                    onFavoriteClick = { movieId, isFavorite ->
                        coroutineScope.launch {
                            savedMovieDao.updateFavorite(
                                movieId = movieId,
                                isFavorite = isFavorite
                            )
                        }
                    },
                    onBackClick = ::openMovies
'''
movie_callbacks_new = '''                    onFavoriteClick = { movieId, isFavorite ->
                        coroutineScope.launch {
                            savedMovieDao.updateFavorite(
                                movieId = movieId,
                                isFavorite = isFavorite
                            )
                        }
                    },
                    onChangeRating = { movieId, rating ->
                        coroutineScope.launch {
                            savedMovieDao.updateRating(
                                movieId = movieId,
                                userRating = rating.coerceIn(0, 10)
                            )
                        }
                    },
                    onRemoveMovie = { movieId ->
                        coroutineScope.launch {
                            savedMovieDao.deleteById(movieId)
                        }
                    },
                    onBackClick = ::openMovies
'''
text = replace_exact(text, movie_callbacks_old, movie_callbacks_new, expected=2)
series_callbacks_old = '''                    onFavoriteClick = { seriesId, isFavorite ->
                        coroutineScope.launch {
                            savedSeriesDao.updateFavorite(
                                seriesId = seriesId,
                                isFavorite = isFavorite
                            )
                        }
                    },
                    onRemoveSeries = { seriesId ->
'''
series_callbacks_new = '''                    onFavoriteClick = { seriesId, isFavorite ->
                        coroutineScope.launch {
                            savedSeriesDao.updateFavorite(
                                seriesId = seriesId,
                                isFavorite = isFavorite
                            )
                        }
                    },
                    onChangeRating = { seriesId, rating ->
                        coroutineScope.launch {
                            savedSeriesDao.updateRating(
                                seriesId = seriesId,
                                userRating = rating.coerceIn(0, 10)
                            )
                        }
                    },
                    onRemoveSeries = { seriesId ->
'''
text = replace_exact(text, series_callbacks_old, series_callbacks_new)
path.write_text(text, encoding="utf-8")


# Movie search screen callback propagation.
path = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/MovieSearchScreen.kt"
text = path.read_text(encoding="utf-8")
text = replace_exact(
    text,
    '''    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    onBackClick: () -> Unit
''',
    '''    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    onChangeRating: (movieId: Int, rating: Int) -> Unit,
    onRemoveMovie: (movieId: Int) -> Unit,
    onBackClick: () -> Unit
'''
)
text = replace_exact(
    text,
    '''                onMovieClick = onMovieClick,
                onFavoriteClick = onFavoriteClick,
                emptyMessage = when {
''',
    '''                onMovieClick = onMovieClick,
                onFavoriteClick = onFavoriteClick,
                onChangeRating = onChangeRating,
                onRemoveMovie = onRemoveMovie,
                emptyMessage = when {
'''
)
path.write_text(text, encoding="utf-8")


# Movie card menu and dialogs.
path = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/MovieListScreen.kt"
text = path.read_text(encoding="utf-8")
text = replace_exact(
    text,
    "import androidx.compose.material3.Card\n",
    "import androidx.compose.material3.AlertDialog\nimport androidx.compose.material3.Card\n"
)
text = replace_exact(
    text,
    '''    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    onBackClick: () -> Unit
''',
    '''    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    onChangeRating: (movieId: Int, rating: Int) -> Unit,
    onRemoveMovie: (movieId: Int) -> Unit,
    onBackClick: () -> Unit
'''
)
text = replace_exact(
    text,
    '''            onMovieClick = onMovieClick,
            onFavoriteClick = onFavoriteClick,
            modifier = Modifier.fillMaxSize()
''',
    '''            onMovieClick = onMovieClick,
            onFavoriteClick = onFavoriteClick,
            onChangeRating = onChangeRating,
            onRemoveMovie = onRemoveMovie,
            modifier = Modifier.fillMaxSize()
'''
)
text = replace_exact(
    text,
    '''    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    modifier: Modifier = Modifier,
''',
    '''    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    onChangeRating: (movieId: Int, rating: Int) -> Unit,
    onRemoveMovie: (movieId: Int) -> Unit,
    modifier: Modifier = Modifier,
'''
)
text = replace_exact(
    text,
    '''                onFavoriteClick = {
                    onFavoriteClick(
                        movie.id,
                        !movie.isFavorite
                    )
                }
''',
    '''                onFavoriteClick = {
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
'''
)
text = replace_exact(
    text,
    '''    movie: SavedMovieEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
''',
    '''    movie: SavedMovieEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onChangeRating: (Int) -> Unit,
    onRemoveMovie: () -> Unit
'''
)
text = replace_exact(
    text,
    '''    var hasError by remember(posterUrl) {
        mutableStateOf(false)
    }

    Card(
''',
    '''    var hasError by remember(posterUrl) {
        mutableStateOf(false)
    }
    var showManageDialog by remember(movie.id) {
        mutableStateOf(false)
    }
    var showRatingDialog by remember(movie.id) {
        mutableStateOf(false)
    }

    Card(
'''
)
text = replace_exact(
    text,
    '''            UserRatingBadge(
                rating = movie.userRating,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        }
    }
}
''',
    '''            UserRatingBadge(
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
'''
)
path.write_text(text, encoding="utf-8")


# Series manage dialog rating option.
path = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesLibraryScreen.kt"
text = path.read_text(encoding="utf-8")
text = replace_exact(
    text,
    '''    onStatusChange: (seriesId: Int, status: SeriesStatus) -> Unit,
    onFavoriteClick: (seriesId: Int, isFavorite: Boolean) -> Unit,
    onRemoveSeries: (seriesId: Int) -> Unit,
''',
    '''    onStatusChange: (seriesId: Int, status: SeriesStatus) -> Unit,
    onFavoriteClick: (seriesId: Int, isFavorite: Boolean) -> Unit,
    onChangeRating: (seriesId: Int, rating: Int) -> Unit,
    onRemoveSeries: (seriesId: Int) -> Unit,
'''
)
text = replace_exact(
    text,
    '''    var managedSeries by remember {
        mutableStateOf<SavedSeriesEntity?>(null)
    }

    val applicationContext = LocalContext.current.applicationContext
''',
    '''    var managedSeries by remember {
        mutableStateOf<SavedSeriesEntity?>(null)
    }
    var ratingSeries by remember {
        mutableStateOf<SavedSeriesEntity?>(null)
    }

    val applicationContext = LocalContext.current.applicationContext
'''
)
text = replace_exact(
    text,
    '''    managedSeries?.let { series ->
        LibraryManageDialog(
            series = series,
            onStatusChange = { status ->
                onStatusChange(series.id, status)
                markFinishedEpisodes(
                    seriesId = series.id,
                    status = status
                )
                managedSeries = null
            },
            onRemove = {
                onRemoveSeries(series.id)
                managedSeries = null
            },
            onCancel = {
                managedSeries = null
            }
        )
    }
''',
    '''    managedSeries?.let { series ->
        LibraryManageDialog(
            series = series,
            onStatusChange = { status ->
                onStatusChange(series.id, status)
                markFinishedEpisodes(
                    seriesId = series.id,
                    status = status
                )
                managedSeries = null
            },
            onChangeRating = {
                ratingSeries = series
                managedSeries = null
            },
            onRemove = {
                onRemoveSeries(series.id)
                managedSeries = null
            },
            onCancel = {
                managedSeries = null
            }
        )
    }

    ratingSeries?.let { series ->
        UserRatingDialog(
            title = "Rate ${series.name}",
            initialRating = series.userRating,
            onSave = { rating ->
                onChangeRating(series.id, rating)
                ratingSeries = null
            },
            onCancel = {
                ratingSeries = null
            }
        )
    }
'''
)
text = replace_exact(
    text,
    '''private fun LibraryManageDialog(
    series: SavedSeriesEntity,
    onStatusChange: (SeriesStatus) -> Unit,
    onRemove: () -> Unit,
    onCancel: () -> Unit
''',
    '''private fun LibraryManageDialog(
    series: SavedSeriesEntity,
    onStatusChange: (SeriesStatus) -> Unit,
    onChangeRating: () -> Unit,
    onRemove: () -> Unit,
    onCancel: () -> Unit
'''
)
text = replace_exact(
    text,
    '''                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onRemove) {
''',
    '''                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onChangeRating) {
                    Text(
                        if (series.userRating in 1..10) {
                            "Change rating"
                        } else {
                            "Add rating"
                        }
                    )
                }
                TextButton(onClick = onRemove) {
'''
)
path.write_text(text, encoding="utf-8")


# Book manage dialog rating option.
path = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/BooksMenuScreen.kt"
text = path.read_text(encoding="utf-8")
text = replace_exact(
    text,
    '''        onSaveReadingState = { book, status, progress ->
            updateBook(book, status, progress)
        },
        onRemoveBook = { bookKey ->
''',
    '''        onSaveReadingState = { book, status, progress ->
            updateBook(book, status, progress)
        },
        onChangeRating = { bookKey, rating ->
            coroutineScope.launch {
                savedBookDao.updateRating(
                    bookKey = bookKey,
                    userRating = rating.coerceIn(0, 10)
                )
            }
        },
        onRemoveBook = { bookKey ->
'''
)
text = replace_exact(
    text,
    '''    onSaveReadingState: (
        book: SavedBookEntity,
        status: BookStatus,
        progress: Int
    ) -> Unit,
    onRemoveBook: (String) -> Unit,
''',
    '''    onSaveReadingState: (
        book: SavedBookEntity,
        status: BookStatus,
        progress: Int
    ) -> Unit,
    onChangeRating: (bookKey: String, rating: Int) -> Unit,
    onRemoveBook: (String) -> Unit,
'''
)
text = replace_exact(
    text,
    '''    var managedBook by remember {
        mutableStateOf<SavedBookEntity?>(null)
    }

    val coroutineScope = rememberCoroutineScope()
''',
    '''    var managedBook by remember {
        mutableStateOf<SavedBookEntity?>(null)
    }
    var ratingBook by remember {
        mutableStateOf<SavedBookEntity?>(null)
    }

    val coroutineScope = rememberCoroutineScope()
'''
)
text = replace_exact(
    text,
    '''    managedBook?.let { book ->
        BookManageDialog(
            book = book,
            onSave = { status, progress ->
                onSaveReadingState(book, status, progress)
                managedBook = null
            },
            onRemove = {
                onRemoveBook(book.key)
                managedBook = null
            },
            onCancel = {
                managedBook = null
            }
        )
    }
''',
    '''    managedBook?.let { book ->
        BookManageDialog(
            book = book,
            onSave = { status, progress ->
                onSaveReadingState(book, status, progress)
                managedBook = null
            },
            onChangeRating = {
                ratingBook = book
                managedBook = null
            },
            onRemove = {
                onRemoveBook(book.key)
                managedBook = null
            },
            onCancel = {
                managedBook = null
            }
        )
    }

    ratingBook?.let { book ->
        UserRatingDialog(
            title = "Rate ${book.title}",
            initialRating = book.userRating,
            onSave = { rating ->
                onChangeRating(book.key, rating)
                ratingBook = null
            },
            onCancel = {
                ratingBook = null
            }
        )
    }
'''
)
text = replace_exact(
    text,
    '''private fun BookManageDialog(
    book: SavedBookEntity,
    onSave: (status: BookStatus, progress: Int) -> Unit,
    onRemove: () -> Unit,
    onCancel: () -> Unit
''',
    '''private fun BookManageDialog(
    book: SavedBookEntity,
    onSave: (status: BookStatus, progress: Int) -> Unit,
    onChangeRating: () -> Unit,
    onRemove: () -> Unit,
    onCancel: () -> Unit
'''
)
text = replace_exact(
    text,
    '''                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onRemove) {
''',
    '''                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onChangeRating) {
                    Text(
                        if (book.userRating in 1..10) {
                            "Change rating"
                        } else {
                            "Add rating"
                        }
                    )
                }
                TextButton(onClick = onRemove) {
'''
)
text = replace_exact(
    text,
    '''            onSave = { status, progress ->
                onSaveReadingState(status, progress)
                showManageDialog = false
            },
            onRemove = {
''',
    '''            onSave = { status, progress ->
                onSaveReadingState(status, progress)
                showManageDialog = false
            },
            onChangeRating = {
                showManageDialog = false
                showRatingDialog = true
            },
            onRemove = {
'''
)
path.write_text(text, encoding="utf-8")
