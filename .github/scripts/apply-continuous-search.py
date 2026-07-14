from pathlib import Path

ROOT = Path('app/src/main/java/com/example/phils_osophy/ui/screens')


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f'{label}: expected one match for {old!r}, found {count}')
    return text.replace(old, new, 1)


def replace_range(text: str, start: str, end: str, replacement: str, label: str) -> str:
    start_index = text.find(start)
    if start_index < 0:
        raise RuntimeError(f'{label}: start marker not found')
    end_index = text.find(end, start_index)
    if end_index < 0:
        raise RuntimeError(f'{label}: end marker not found')
    return text[:start_index] + replacement + text[end_index:]


def patch_movie() -> None:
    path = ROOT / 'MovieSearchScreen.kt'
    text = path.read_text()
    for old in (
        'import androidx.compose.foundation.text.KeyboardActions\n',
        'import androidx.compose.foundation.text.KeyboardOptions\n',
        'import androidx.compose.runtime.rememberCoroutineScope\n',
        'import androidx.compose.ui.text.input.ImeAction\n',
    ):
        text = replace_once(text, old, '', path.name)
    text = replace_once(text, 'import androidx.compose.material3.MaterialTheme\n',
        'import androidx.compose.material3.IconButton\nimport androidx.compose.material3.MaterialTheme\n', path.name)
    text = replace_once(text, 'import androidx.compose.runtime.Composable\n',
        'import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.LaunchedEffect\n', path.name)
    text = replace_once(text, 'import kotlinx.coroutines.launch\n',
        'import kotlinx.coroutines.CancellationException\n', path.name)
    text = replace_once(text, '    val coroutineScope = rememberCoroutineScope()\n', '', path.name)

    effect = '''    LaunchedEffect(query, showFavoritesOnly) {
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
    }'''
    text = replace_range(text, '    val searchMovies = {', '\n\n    Column(', effect, path.name)

    field = '''        AnimatedVisibility(visible = isSearchBarVisible) {
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
        }'''
    text = replace_range(text, '        AnimatedVisibility(visible = isSearchBarVisible) {',
        '\n\n        if (showFavoritesOnly || !hasSearched) {', field, path.name)
    path.write_text(text)


def patch_series() -> None:
    path = ROOT / 'SeriesLibraryScreen.kt'
    text = path.read_text()
    for old in (
        'import androidx.compose.foundation.text.KeyboardActions\n',
        'import androidx.compose.foundation.text.KeyboardOptions\n',
        'import androidx.compose.ui.text.input.ImeAction\n',
    ):
        text = replace_once(text, old, '', path.name)
    text = replace_once(text, 'import androidx.compose.material3.MaterialTheme\n',
        'import androidx.compose.material3.IconButton\nimport androidx.compose.material3.MaterialTheme\n', path.name)
    text = replace_once(text, 'import androidx.compose.runtime.Composable\n',
        'import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.LaunchedEffect\n', path.name)
    text = replace_once(text, 'import kotlinx.coroutines.launch\n',
        'import kotlinx.coroutines.CancellationException\nimport kotlinx.coroutines.launch\n', path.name)

    effect = '''    LaunchedEffect(query, showFavoritesOnly) {
        val submittedQuery = query.trim()

        if (submittedQuery.isBlank()) {
            resetSearch()
            return@LaunchedEffect
        }

        hasSearched = true
        errorMessage = null
        remoteSearchResults = emptyList()

        if (showFavoritesOnly) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        try {
            remoteSearchResults = TmdbClient.api
                .searchSeries(submittedQuery)
                .results
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            remoteSearchResults = emptyList()
            errorMessage = exception.localizedMessage
                ?: "Series search failed."
        } finally {
            if (query.trim() == submittedQuery && !showFavoritesOnly) {
                isLoading = false
            }
        }
    }'''
    text = replace_range(text, '    fun searchSeries() {', '\n\n    Column(', effect, path.name)

    field = '''        AnimatedVisibility(visible = isSearchBarVisible) {
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
                                "Search for a series"
                            }
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Text(
                                    text = "×",
                                    color = LibraryFavoriteColor,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }'''
    text = replace_range(text, '        AnimatedVisibility(visible = isSearchBarVisible) {',
        '\n\n        if (!hasSearched) {', field, path.name)
    path.write_text(text)


def patch_books() -> None:
    path = ROOT / 'BooksMenuScreen.kt'
    text = path.read_text()
    for old in (
        'import androidx.compose.foundation.text.KeyboardActions\n',
        'import androidx.compose.foundation.text.KeyboardOptions\n',
        'import androidx.compose.ui.text.input.ImeAction\n',
    ):
        text = replace_once(text, old, '', path.name)
    text = replace_once(text, 'import androidx.compose.material3.HorizontalDivider\n',
        'import androidx.compose.material3.HorizontalDivider\nimport androidx.compose.material3.IconButton\n', path.name)
    text = replace_once(text, 'import androidx.compose.runtime.Composable\n',
        'import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.LaunchedEffect\n', path.name)
    text = replace_once(text, 'import kotlinx.coroutines.launch\n',
        'import kotlinx.coroutines.CancellationException\nimport kotlinx.coroutines.launch\n', path.name)
    text = replace_once(text,
        '    val coroutineScope = rememberCoroutineScope()\n    val searchBarScrollConnection = remember {\n',
        '    val searchBarScrollConnection = remember {\n', path.name)

    effect = '''    LaunchedEffect(query, showFavoritesOnly) {
        val submittedQuery = query.trim()

        if (submittedQuery.isBlank()) {
            resetSearch()
            return@LaunchedEffect
        }

        hasSearched = true
        errorMessage = null
        remoteSearchResults = emptyList()

        if (showFavoritesOnly) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        try {
            remoteSearchResults = OpenLibraryClient.api
                .searchBooks(submittedQuery)
                .docs
                .filter { book ->
                    book.key.isNotBlank() && book.title.isNotBlank()
                }
                .distinctBy { book -> book.key }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            remoteSearchResults = emptyList()
            errorMessage = exception.localizedMessage
                ?: "Book search failed."
        } finally {
            if (query.trim() == submittedQuery && !showFavoritesOnly) {
                isLoading = false
            }
        }
    }'''
    text = replace_range(text, '    fun searchBooks() {', '\n\n    Column(', effect, path.name)

    field = '''        AnimatedVisibility(visible = isSearchBarVisible) {
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
                                "Search for a book"
                            }
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Text(
                                    text = "×",
                                    color = BookFavoriteColor,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }'''
    text = replace_range(text, '        AnimatedVisibility(visible = isSearchBarVisible) {',
        '\n\n        if (!hasSearched) {', field, path.name)
    path.write_text(text)


patch_movie()
patch_series()
patch_books()
