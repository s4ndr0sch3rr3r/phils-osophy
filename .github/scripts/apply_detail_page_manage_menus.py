from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_once(text: str, old: str, new: str, path: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one match, found {count}: {old[:80]!r}")
    return text.replace(old, new, 1)


def write(path: str, text: str) -> None:
    (ROOT / path).write_text(text, encoding="utf-8")


# App wiring: list screens no longer own management actions; Series detail does.
path = "app/src/main/java/com/example/phils_osophy/App.kt"
text = (ROOT / path).read_text(encoding="utf-8")
for block in [
    '''                    onChangeRating = { movieId, rating ->
                        coroutineScope.launch {
                            savedMovieDao.updateRating(
                                movieId = movieId,
                                userRating = rating.coerceIn(0, USER_RATING_MAX)
                            )
                        }
                    },
                    onRemoveMovie = { movieId ->
                        coroutineScope.launch {
                            savedMovieDao.deleteById(movieId)
                        }
                    },
''',
]:
    # This block occurs once in MoviesMenu and once in MoviesList.
    if text.count(block) != 2:
        raise RuntimeError(f"{path}: expected two movie list management callback blocks")
    text = text.replace(block, "", 2)

text = replace_once(
    text,
    '''                    onStatusChange = { seriesId, status ->
                        coroutineScope.launch {
                            savedSeriesDao.updateStatus(
                                seriesId = seriesId,
                                status = status.name
                            )
                        }
                    },
''',
    "",
    path,
)
text = replace_once(
    text,
    '''                    onChangeRating = { seriesId, rating ->
                        coroutineScope.launch {
                            savedSeriesDao.updateRating(
                                seriesId = seriesId,
                                userRating = rating.coerceIn(0, USER_RATING_MAX)
                            )
                        }
                    },
                    onRemoveSeries = { seriesId ->
                        coroutineScope.launch {
                            watchedEpisodeDao.deleteForSeries(seriesId)
                            savedSeriesDao.deleteById(seriesId)
                        }
                    },
''',
    "",
    path,
)
text = replace_once(
    text,
    '''                        onFavoriteClick = { isFavorite ->
                            coroutineScope.launch {
                                savedSeriesDao.updateFavorite(
                                    seriesId = selectedSeries.id,
                                    isFavorite = isFavorite
                                )
                            }
                        },
                        onChangeRating = { rating ->
''',
    '''                        onFavoriteClick = { isFavorite ->
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
''',
    path,
)
write(path, text)


# Movie search/list signatures and card UI.
path = "app/src/main/java/com/example/phils_osophy/ui/screens/MovieSearchScreen.kt"
text = (ROOT / path).read_text(encoding="utf-8")
text = replace_once(
    text,
    '''    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    onChangeRating: (movieId: Int, rating: Int) -> Unit,
    onRemoveMovie: (movieId: Int) -> Unit,
    onBackClick: () -> Unit
''',
    '''    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    onBackClick: () -> Unit
''',
    path,
)
text = replace_once(
    text,
    '''                onMovieClick = onMovieClick,
                onFavoriteClick = onFavoriteClick,
                onChangeRating = onChangeRating,
                onRemoveMovie = onRemoveMovie,
                emptyMessage = when {
''',
    '''                onMovieClick = onMovieClick,
                onFavoriteClick = onFavoriteClick,
                emptyMessage = when {
''',
    path,
)
write(path, text)

path = "app/src/main/java/com/example/phils_osophy/ui/screens/MovieListScreen.kt"
text = (ROOT / path).read_text(encoding="utf-8")
for line in [
    "import androidx.compose.material3.AlertDialog\n",
    "import androidx.compose.material3.OutlinedTextField\n",
    "import androidx.compose.runtime.rememberCoroutineScope\n",
    "import androidx.compose.ui.platform.LocalContext\n",
    "import com.example.phils_osophy.data.local.PhilsOsophyDatabase\n",
    "import kotlinx.coroutines.launch\n",
]:
    text = text.replace(line, "")
text = replace_once(
    text,
    '''    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    onChangeRating: (movieId: Int, rating: Int) -> Unit,
    onRemoveMovie: (movieId: Int) -> Unit,
    onBackClick: () -> Unit
''',
    '''    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    onBackClick: () -> Unit
''',
    path,
)
text = replace_once(
    text,
    '''            onMovieClick = onMovieClick,
            onFavoriteClick = onFavoriteClick,
            onChangeRating = onChangeRating,
            onRemoveMovie = onRemoveMovie,
            modifier = Modifier.fillMaxSize()
''',
    '''            onMovieClick = onMovieClick,
            onFavoriteClick = onFavoriteClick,
            modifier = Modifier.fillMaxSize()
''',
    path,
)
text = replace_once(
    text,
    '''    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    onChangeRating: (movieId: Int, rating: Int) -> Unit,
    onRemoveMovie: (movieId: Int) -> Unit,
    modifier: Modifier = Modifier,
''',
    '''    onMovieClick: (Int) -> Unit,
    onFavoriteClick: (movieId: Int, isFavorite: Boolean) -> Unit,
    modifier: Modifier = Modifier,
''',
    path,
)
text = replace_once(
    text,
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
''',
    '''                onFavoriteClick = {
                    onFavoriteClick(
                        movie.id,
                        !movie.isFavorite
                    )
                }
''',
    path,
)
text = replace_once(
    text,
    '''    movie: SavedMovieEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onChangeRating: (Int) -> Unit,
    onRemoveMovie: () -> Unit
''',
    '''    movie: SavedMovieEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
''',
    path,
)
start = text.index("    var showManageDialog by remember(movie.id)")
end = text.index("    Column(modifier = Modifier.fillMaxWidth())", start)
text = text[:start] + text[end:]
text = replace_once(
    text,
    '''
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
''',
    "\n",
    path,
)
dialog_start = text.index("\n\n    if (showManageDialog)")
text = text[:dialog_start] + "\n}\n"
write(path, text)


# Align the existing Movie detail menu with the global alien button.
path = "app/src/main/java/com/example/phils_osophy/ui/screens/MovieDetailScreen.kt"
text = (ROOT / path).read_text(encoding="utf-8")
text = replace_once(
    text,
    ".padding(end = 8.dp, top = 8.dp)",
    ".padding(end = 66.dp, top = 20.dp)",
    path,
)
write(path, text)


# Series overview loses management controls; detail gains status/removal management.
path = "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesLibraryScreen.kt"
text = (ROOT / path).read_text(encoding="utf-8")
text = text.replace("import com.example.phils_osophy.data.local.PhilsOsophyDatabase\n", "")
text = replace_once(
    text,
    '''    onSeriesClick: (Int) -> Unit,
    onStatusChange: (seriesId: Int, status: SeriesStatus) -> Unit,
    onFavoriteClick: (seriesId: Int, isFavorite: Boolean) -> Unit,
    onChangeRating: (seriesId: Int, rating: Int) -> Unit,
    onRemoveSeries: (seriesId: Int) -> Unit,
    onBackClick: () -> Unit
''',
    '''    onSeriesClick: (Int) -> Unit,
    onFavoriteClick: (seriesId: Int, isFavorite: Boolean) -> Unit,
    onBackClick: () -> Unit
''',
    path,
)
for block in [
    '''    var managedSeries by remember {
        mutableStateOf<SavedSeriesEntity?>(null)
    }
    var ratingSeries by remember {
        mutableStateOf<SavedSeriesEntity?>(null)
    }
    var commentSeries by remember {
        mutableStateOf<SavedSeriesEntity?>(null)
    }

''',
    '''    val savedSeriesDao = remember(applicationContext) {
        PhilsOsophyDatabase
            .getInstance(applicationContext)
            .savedSeriesDao()
    }
''',
]:
    text = replace_once(text, block, "", path)
text = text.replace(",\n                        onManageClick = { series -> managedSeries = series }", "")
managed_start = text.index("\n    managedSeries?.let")
managed_end = text.index("\n}\n\n@Composable\nprivate fun LibrarySection", managed_start)
text = text[:managed_start] + text[managed_end:]
text = replace_once(
    text,
    '''    onFavoriteClick: (
        seriesId: Int,
        isFavorite: Boolean
    ) -> Unit,
    onManageClick: (SavedSeriesEntity) -> Unit
''',
    '''    onFavoriteClick: (
        seriesId: Int,
        isFavorite: Boolean
    ) -> Unit
''',
    path,
)
text = text.replace(",\n                                onManageClick = {\n                                    onManageClick(savedSeries)\n                                }", "")
text = text.replace(",\n                        onManageClick = {\n                            onManageClick(savedSeries)\n                        }", "")
text = replace_once(
    text,
    '''    series: SavedSeriesEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onManageClick: () -> Unit
''',
    '''    series: SavedSeriesEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
''',
    path,
)
text = replace_once(
    text,
    '''
                Text(
                    text = "•••",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(onClick = onManageClick)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
''',
    "\n",
    path,
)
manage_dialog_start = text.index("\n@Composable\nprivate fun LibraryManageDialog")
manage_dialog_end = text.index("\n@Composable\nprivate fun LibraryStatusChoices", manage_dialog_start)
text = text[:manage_dialog_start] + text[manage_dialog_end:]
write(path, text)

path = "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesDetailScreen.kt"
text = (ROOT / path).read_text(encoding="utf-8")
text = text.replace(
    "import androidx.compose.material3.Card\n",
    "import androidx.compose.material3.AlertDialog\nimport androidx.compose.material3.Button\nimport androidx.compose.material3.Card\n",
)
text = text.replace(
    "import androidx.compose.material3.MaterialTheme\n",
    "import androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.RadioButton\n",
)
text = text.replace(
    "import com.example.phils_osophy.data.local.SeriesCompletionTracker\n",
    "import com.example.phils_osophy.data.local.SeriesCompletionTracker\nimport com.example.phils_osophy.data.local.SeriesStatus\n",
)
text = replace_once(
    text,
    '''    onBackClick: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit,
    onChangeRating: (Int) -> Unit,
''',
    '''    onBackClick: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit,
    onStatusChange: (SeriesStatus) -> Unit,
    onRemoveSeries: () -> Unit,
    onChangeRating: (Int) -> Unit,
''',
    path,
)
text = replace_once(
    text,
    '''    var selectedTab by remember(series.id) {
        mutableStateOf(0)
    }
''',
    '''    var selectedTab by remember(series.id) {
        mutableStateOf(0)
    }
    var showManageDialog by remember(series.id) {
        mutableStateOf(false)
    }
''',
    path,
)
text = replace_once(
    text,
    '''            isFavorite = series.isFavorite,
            onBackClick = onBackClick,
            onFavoriteClick = onFavoriteClick
''',
    '''            isFavorite = series.isFavorite,
            onBackClick = onBackClick,
            onFavoriteClick = onFavoriteClick,
            onManageClick = { showManageDialog = true }
''',
    path,
)
text = replace_once(
    text,
    '''    }

}

@Composable
private fun SeriesHero(
''',
    '''    }

    if (showManageDialog) {
        SeriesDetailManageDialog(
            series = series,
            onSave = { status ->
                onStatusChange(status)
                if (status == SeriesStatus.FINISHED) {
                    coroutineScope.launch {
                        completionTracker.markAllEpisodesWatched(series.id)
                    }
                }
                showManageDialog = false
            },
            onRemove = {
                onRemoveSeries()
                showManageDialog = false
            },
            onCancel = { showManageDialog = false }
        )
    }
}

@Composable
private fun SeriesHero(
''',
    path,
)
text = replace_once(
    text,
    '''    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit
''',
    '''    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit,
    onManageClick: () -> Unit
''',
    path,
)
text = replace_once(
    text,
    '''                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 8.dp, top = 8.dp)
''',
    '''                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 116.dp, top = 20.dp)
''',
    path,
)
hero_column_marker = '''        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
'''
menu_block = '''        TextButton(
            onClick = onManageClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 66.dp, top = 20.dp)
        ) {
            Text(
                text = "•••",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

'''
text = replace_once(text, hero_column_marker, menu_block + hero_column_marker, path)
insert_before = "\n@Composable\nprivate fun SeriesInfoTab("
dialog = '''
@Composable
private fun SeriesDetailManageDialog(
    series: SavedSeriesEntity,
    onSave: (SeriesStatus) -> Unit,
    onRemove: () -> Unit,
    onCancel: () -> Unit
) {
    var selectedStatus by remember(series.id, series.status) {
        mutableStateOf(SeriesStatus.fromStorage(series.status))
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(series.name) },
        text = {
            Column {
                Text("Move to")
                Spacer(modifier = Modifier.height(8.dp))
                SeriesStatus.values().forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatus = status }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = status == selectedStatus,
                            onClick = { selectedStatus = status }
                        )
                        Text(seriesStatusLabel(status))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onRemove) {
                    Text(
                        text = "Remove series",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedStatus) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

private fun seriesStatusLabel(status: SeriesStatus): String = when (status) {
    SeriesStatus.IN_PROGRESS -> "En cours"
    SeriesStatus.FINISHED -> "Séries terminées"
    SeriesStatus.TO_WATCH -> "Séries à regarder"
    SeriesStatus.STOPPED -> "Séries arrêtées"
}
'''
text = replace_once(text, insert_before, dialog + insert_before, path)
write(path, text)


# Books overview loses management controls; detail gets top-right management next to alien.
path = "app/src/main/java/com/example/phils_osophy/ui/screens/BooksMenuScreen.kt"
text = (ROOT / path).read_text(encoding="utf-8")
text = replace_once(
    text,
    '''        onFavoriteClick = { bookKey, isFavorite ->
            coroutineScope.launch {
                savedBookDao.updateFavorite(
                    bookKey = bookKey,
                    isFavorite = isFavorite
                )
            }
        },
        onSaveReadingState = { book, status, progress ->
            updateBook(book, status, progress)
        },
        onChangeRating = { bookKey, rating ->
            coroutineScope.launch {
                savedBookDao.updateRating(
                    bookKey = bookKey,
                    userRating = rating.coerceIn(0, USER_RATING_MAX)
                )
            }
        },
        onSaveComment = ::updateComment,
        onRemoveBook = { bookKey ->
            coroutineScope.launch {
                savedBookDao.deleteByKey(bookKey)
            }
        },
        onBackClick = onBackClick
''',
    '''        onFavoriteClick = { bookKey, isFavorite ->
            coroutineScope.launch {
                savedBookDao.updateFavorite(
                    bookKey = bookKey,
                    isFavorite = isFavorite
                )
            }
        },
        onBackClick = onBackClick
''',
    path,
)
text = replace_once(
    text,
    '''    onFavoriteClick: (bookKey: String, isFavorite: Boolean) -> Unit,
    onSaveReadingState: (
        book: SavedBookEntity,
        status: BookStatus,
        progress: Int
    ) -> Unit,
    onChangeRating: (bookKey: String, rating: Int) -> Unit,
    onSaveComment: (bookKey: String, comment: String) -> Unit,
    onRemoveBook: (String) -> Unit,
    onBackClick: () -> Unit
''',
    '''    onFavoriteClick: (bookKey: String, isFavorite: Boolean) -> Unit,
    onBackClick: () -> Unit
''',
    path,
)
state_block = '''    var managedBook by remember {
        mutableStateOf<SavedBookEntity?>(null)
    }
    var ratingBook by remember {
        mutableStateOf<SavedBookEntity?>(null)
    }
    var commentBook by remember {
        mutableStateOf<SavedBookEntity?>(null)
    }

'''
text = replace_once(text, state_block, "", path)
text = text.replace(",\n                        onManageClick = { book -> managedBook = book }", "")
managed_start = text.index("\n    managedBook?.let")
managed_end = text.index("\n}\n\n@Composable\nprivate fun BookShelf", managed_start)
text = text[:managed_start] + text[managed_end:]
text = replace_once(
    text,
    '''    books: List<SavedBookEntity>,
    onBookClick: (String) -> Unit,
    onFavoriteClick: (bookKey: String, isFavorite: Boolean) -> Unit,
    onManageClick: (SavedBookEntity) -> Unit
''',
    '''    books: List<SavedBookEntity>,
    onBookClick: (String) -> Unit,
    onFavoriteClick: (bookKey: String, isFavorite: Boolean) -> Unit
''',
    path,
)
text = replace_once(
    text,
    ''',
                                onManageClick = {
                                    onManageClick(book)
                                }
''',
    "\n",
    path,
)
text = replace_once(
    text,
    '''    book: SavedBookEntity,
    onBookClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onManageClick: () -> Unit
''',
    '''    book: SavedBookEntity,
    onBookClick: () -> Unit,
    onFavoriteClick: () -> Unit
''',
    path,
)
text = replace_once(
    text,
    '''
            Text(
                text = "•••",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(onClick = onManageClick)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                color = Color.White,
                fontSize = 16.sp
            )
''',
    "\n",
    path,
)
# Wrap the detail content so fixed top actions can align with the global alien.
text = replace_once(
    text,
    '''    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
''',
    '''    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
''',
    path,
)
# Remove favorite from the content row; it becomes a fixed header action.
favorite_block = '''            TextButton(
                onClick = {
                    onFavoriteClick(!book.isFavorite)
                }
            ) {
                Text(
                    text = if (book.isFavorite) "♥" else "♡",
                    color = if (book.isFavorite) {
                        BookFavoriteColor
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 30.sp
                )
            }
'''
text = replace_once(text, favorite_block, "", path)
manage_item = '''            item {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        showManageDialog = true
                    }
                ) {
                    Text("Manage book")
                }
            }
'''
text = replace_once(text, manage_item, "", path)
# Close the content Column and add fixed favorite/menu actions before the outer Box closes.
text = replace_once(
    text,
    '''        }
    }

    if (showManageDialog) {
''',
    '''        }

        TextButton(
            onClick = { onFavoriteClick(!book.isFavorite) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 116.dp, top = 20.dp)
        ) {
            Text(
                text = if (book.isFavorite) "♥" else "♡",
                color = if (book.isFavorite) BookFavoriteColor else MaterialTheme.colorScheme.onSurface,
                fontSize = 30.sp
            )
        }

        TextButton(
            onClick = { showManageDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 66.dp, top = 20.dp)
        ) {
            Text(
                text = "•••",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showManageDialog) {
''',
    path,
)
write(path, text)

print("Applied detail-page management menus")
