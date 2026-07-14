package com.example.phils_osophy.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phils_osophy.data.local.BookStatus
import com.example.phils_osophy.data.local.PhilsOsophyDatabase
import com.example.phils_osophy.data.local.SavedBookEntity
import com.example.phils_osophy.data.local.toSavedBookEntity
import com.example.phils_osophy.data.remote.BookDto
import com.example.phils_osophy.data.remote.OpenLibraryClient
import com.example.phils_osophy.ui.components.FavoriteIcon
import com.example.phils_osophy.ui.util.formatStoredDate
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val OPEN_LIBRARY_COVER_BASE_URL =
    "https://covers.openlibrary.org/b/id"

private val BookFavoriteColor = Color(0xFFE53935)

@Suppress("UNUSED_PARAMETER")
@Composable
fun BooksMenuScreen(
    onBackClick: () -> Unit,
    onInProgressClick: () -> Unit,
    onFinishedClick: () -> Unit,
    onToReadClick: () -> Unit,
    onAbandonedClick: () -> Unit
) {
    val applicationContext = LocalContext.current.applicationContext
    val savedBookDao = remember(applicationContext) {
        PhilsOsophyDatabase
            .getInstance(applicationContext)
            .savedBookDao()
    }
    val coroutineScope = rememberCoroutineScope()

    val inProgressBooks by remember(savedBookDao) {
        savedBookDao.observeInProgress()
    }.collectAsState(initial = emptyList())
    val finishedBooks by remember(savedBookDao) {
        savedBookDao.observeFinished()
    }.collectAsState(initial = emptyList())
    val toReadBooks by remember(savedBookDao) {
        savedBookDao.observeToRead()
    }.collectAsState(initial = emptyList())
    val abandonedBooks by remember(savedBookDao) {
        savedBookDao.observeAbandoned()
    }.collectAsState(initial = emptyList())

    val allSavedBooks = remember(
        inProgressBooks,
        finishedBooks,
        toReadBooks,
        abandonedBooks
    ) {
        (
            inProgressBooks +
                finishedBooks +
                toReadBooks +
                abandonedBooks
            ).distinctBy { book -> book.key }
    }

    var selectedBookKey by remember {
        mutableStateOf<String?>(null)
    }
    val selectedBook = allSavedBooks.firstOrNull { book ->
        book.key == selectedBookKey
    }

    fun updateBook(
        book: SavedBookEntity,
        status: BookStatus,
        progress: Int
    ) {
        val normalizedProgress = normalizeProgress(status, progress)
        val finishedAt = if (status == BookStatus.FINISHED) {
            book.finishedAtEpochMillis ?: System.currentTimeMillis()
        } else {
            null
        }

        coroutineScope.launch {
            savedBookDao.updateReadingState(
                bookKey = book.key,
                status = status.name,
                readingProgressPercent = normalizedProgress,
                finishedAtEpochMillis = finishedAt
            )
        }
    }

    fun updateComment(bookKey: String, comment: String) {
        coroutineScope.launch {
            savedBookDao.updateNote(
                bookKey = bookKey,
                note = comment
            )
        }
    }

    if (selectedBookKey != null) {
        if (selectedBook == null) {
            EmptyPageScreen(
                title = "Book unavailable",
                onBackClick = {
                    selectedBookKey = null
                }
            )
        } else {
            BookDetailScreen(
                book = selectedBook,
                onBackClick = {
                    selectedBookKey = null
                },
                onFavoriteClick = { isFavorite ->
                    coroutineScope.launch {
                        savedBookDao.updateFavorite(
                            bookKey = selectedBook.key,
                            isFavorite = isFavorite
                        )
                    }
                },
                onSaveReadingState = { status, progress ->
                    updateBook(selectedBook, status, progress)
                },
                onChangeRating = { rating ->
                    coroutineScope.launch {
                        savedBookDao.updateRating(
                            bookKey = selectedBook.key,
                            userRating = rating.coerceIn(0, USER_RATING_MAX)
                        )
                    }
                },
                onSaveComment = { comment ->
                    updateComment(selectedBook.key, comment)
                },
                onRemove = {
                    coroutineScope.launch {
                        savedBookDao.deleteByKey(selectedBook.key)
                    }
                    selectedBookKey = null
                }
            )
        }
        return
    }

    BookLibraryScreen(
        inProgressBooks = inProgressBooks,
        finishedBooks = finishedBooks,
        toReadBooks = toReadBooks,
        abandonedBooks = abandonedBooks,
        onAddBook = { book, status ->
            coroutineScope.launch {
                savedBookDao.insert(
                    book.toSavedBookEntity(status = status)
                )
            }
        },
        onBookClick = { bookKey ->
            selectedBookKey = bookKey
        },
        onFavoriteClick = { bookKey, isFavorite ->
            coroutineScope.launch {
                savedBookDao.updateFavorite(
                    bookKey = bookKey,
                    isFavorite = isFavorite
                )
            }
        },
        onBackClick = onBackClick
    )
}

@Composable
private fun BookLibraryScreen(
    inProgressBooks: List<SavedBookEntity>,
    finishedBooks: List<SavedBookEntity>,
    toReadBooks: List<SavedBookEntity>,
    abandonedBooks: List<SavedBookEntity>,
    onAddBook: (BookDto, BookStatus) -> Unit,
    onBookClick: (String) -> Unit,
    onFavoriteClick: (bookKey: String, isFavorite: Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    var query by remember { mutableStateOf("") }
    var remoteSearchResults by remember {
        mutableStateOf<List<BookDto>>(emptyList())
    }
    var isLoading by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var isSearchBarVisible by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingBook by remember { mutableStateOf<BookDto?>(null) }
    val expandedBookSections = remember {
        mutableStateMapOf<String, Boolean>()
    }
    val coroutineScope = rememberCoroutineScope()
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
    val allSavedBooks = remember(
        inProgressBooks,
        finishedBooks,
        toReadBooks,
        abandonedBooks
    ) {
        (
            inProgressBooks +
                finishedBooks +
                toReadBooks +
                abandonedBooks
            ).distinctBy { book -> book.key }
    }
    val savedBookKeys = remember(allSavedBooks) {
        allSavedBooks.map { book -> book.key }.toSet()
    }
    val cleanQuery = query.trim()
    val localSearchResults = if (hasSearched) {
        allSavedBooks.filter { book ->
            (!showFavoritesOnly || book.isFavorite) &&
                (
                    book.title.contains(cleanQuery, ignoreCase = true) ||
                        book.authors.contains(cleanQuery, ignoreCase = true)
                    )
        }
    } else {
        emptyList()
    }
    val visibleRemoteResults = remoteSearchResults.filterNot { book ->
        book.key in savedBookKeys
    }

    fun resetSearch() {
        hasSearched = false
        remoteSearchResults = emptyList()
        errorMessage = null
        isLoading = false
    }

    fun filtered(books: List<SavedBookEntity>): List<SavedBookEntity> =
        books.filter { book ->
            !showFavoritesOnly || book.isFavorite
        }

    fun searchBooks() {
        val submittedQuery = query.trim()
        if (submittedQuery.isBlank() || isLoading) {
            return
        }

        hasSearched = true
        errorMessage = null
        remoteSearchResults = emptyList()

        if (showFavoritesOnly) {
            return
        }

        coroutineScope.launch {
            isLoading = true
            try {
                val results = OpenLibraryClient.api
                    .searchBooks(submittedQuery)
                    .docs
                    .filter { book ->
                        book.key.isNotBlank() && book.title.isNotBlank()
                    }
                    .distinctBy { book -> book.key }
                if (query.trim() == submittedQuery) {
                    remoteSearchResults = results
                }
            } catch (exception: Exception) {
                remoteSearchResults = emptyList()
                errorMessage = exception.localizedMessage
                    ?: "Book search failed."
            } finally {
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
                text = "Books (${allSavedBooks.size})",
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
                    activeColor = BookFavoriteColor,
                    inactiveColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = isSearchBarVisible) {
            Column {
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
                                    "Search for a book"
                                }
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { searchBooks() }
                        )
                    )
                    Button(
                        onClick = { searchBooks() },
                        enabled = query.isNotBlank() && !isLoading
                    ) {
                        Text("Search")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (!hasSearched) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                bookShelfItems(
                    title = "En cours",
                    books = filtered(inProgressBooks),
                    isExpanded = expandedBookSections["En cours"] == true,
                    onToggleExpanded = {
                        expandedBookSections["En cours"] =
                            expandedBookSections["En cours"] != true
                    },
                    onBookClick = onBookClick,
                    onFavoriteClick = onFavoriteClick
                )
                bookShelfItems(
                    title = "Livres terminés",
                    books = filtered(finishedBooks),
                    isExpanded = expandedBookSections["Livres terminés"] == true,
                    onToggleExpanded = {
                        expandedBookSections["Livres terminés"] =
                            expandedBookSections["Livres terminés"] != true
                    },
                    onBookClick = onBookClick,
                    onFavoriteClick = onFavoriteClick
                )
                bookShelfItems(
                    title = "Livres à lire",
                    books = filtered(toReadBooks),
                    isExpanded = expandedBookSections["Livres à lire"] == true,
                    onToggleExpanded = {
                        expandedBookSections["Livres à lire"] =
                            expandedBookSections["Livres à lire"] != true
                    },
                    onBookClick = onBookClick,
                    onFavoriteClick = onFavoriteClick
                )
                bookShelfItems(
                    title = "Livres abandonnés",
                    books = filtered(abandonedBooks),
                    isExpanded = expandedBookSections["Livres abandonnés"] == true,
                    onToggleExpanded = {
                        expandedBookSections["Livres abandonnés"] =
                            expandedBookSections["Livres abandonnés"] != true
                    },
                    onBookClick = onBookClick,
                    onFavoriteClick = onFavoriteClick
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (localSearchResults.isNotEmpty()) {
                    item(key = "local-books-heading") {
                        BookSearchHeading("In your library")
                    }
                    items(
                        items = localSearchResults,
                        key = { book -> "local-${book.key}" }
                    ) { book ->
                        BookSearchResult(
                            title = book.title,
                            authors = book.authors,
                            firstPublishYear = book.firstPublishYear,
                            coverId = book.coverId,
                            onImageClick = {
                                onBookClick(book.key)
                            }
                        )
                    }
                }

                if (!showFavoritesOnly) {
                    item(key = "open-library-books-heading") {
                        BookSearchHeading("Open Library results")
                    }

                    if (isLoading) {
                        item(key = "open-library-books-loading") {
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
                        item(key = "open-library-books-error") {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    items(
                        items = visibleRemoteResults,
                        key = { book -> "remote-${book.key}" }
                    ) { book ->
                        BookSearchResult(
                            title = book.title,
                            authors = book.authorNames.joinToString(", "),
                            firstPublishYear = book.firstPublishYear,
                            coverId = book.coverId,
                            onAddClick = {
                                pendingBook = book
                            }
                        )
                    }
                }

                if (
                    !isLoading &&
                    errorMessage == null &&
                    localSearchResults.isEmpty() &&
                    visibleRemoteResults.isEmpty()
                ) {
                    item(key = "books-empty") {
                        Text("No books found.")
                    }
                }
            }
        }
    }

    pendingBook?.let { book ->
        BookAddDialog(
            book = book,
            onAdd = { status ->
                onAddBook(book, status)
                pendingBook = null
            },
            onCancel = {
                pendingBook = null
            }
        )
    }
}

private fun LazyListScope.bookShelfItems(
    title: String,
    books: List<SavedBookEntity>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onBookClick: (String) -> Unit,
    onFavoriteClick: (bookKey: String, isFavorite: Boolean) -> Unit
) {
    val sectionKey = "book-section-$title"

    item(
        key = "$sectionKey-header",
        contentType = "library-section-header"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isExpanded) "⌃" else "⌄",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    item(
        key = "$sectionKey-top-gap",
        contentType = "library-section-gap"
    ) {
        Spacer(modifier = Modifier.height(10.dp))
    }

    when {
        books.isEmpty() -> {
            item(
                key = "$sectionKey-empty",
                contentType = "library-section-empty"
            ) {
                Text(
                    text = "No books in this category.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        isExpanded -> {
            val rowCount = (books.size + 2) / 3
            items(
                count = rowCount,
                key = { rowIndex ->
                    "$sectionKey-row-${books[rowIndex * 3].key}"
                },
                contentType = { "book-grid-row" }
            ) { rowIndex ->
                val startIndex = rowIndex * 3
                val endIndex = minOf(startIndex + 3, books.size)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    for (bookIndex in startIndex until endIndex) {
                        val book = books[bookIndex]
                        SavedBookCard(
                            book = book,
                            onBookClick = {
                                onBookClick(book.key)
                            },
                            onFavoriteClick = {
                                onFavoriteClick(
                                    book.key,
                                    !book.isFavorite
                                )
                            }
                        )
                    }
                }
            }
        }

        else -> {
            item(
                key = "$sectionKey-preview",
                contentType = "book-horizontal-preview"
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    items(
                        items = books,
                        key = { book -> book.key },
                        contentType = { "book-card" }
                    ) { book ->
                        SavedBookCard(
                            book = book,
                            onBookClick = {
                                onBookClick(book.key)
                            },
                            onFavoriteClick = {
                                onFavoriteClick(
                                    book.key,
                                    !book.isFavorite
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    item(
        key = "$sectionKey-bottom-gap",
        contentType = "library-section-gap"
    ) {
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SavedBookCard(
    book: SavedBookEntity,
    onBookClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Column(modifier = Modifier.width(104.dp)) {
        Box(
            modifier = Modifier
                .size(width = 104.dp, height = 156.dp)
                .clickable(onClick = onBookClick)
        ) {
            BookCover(
                coverId = book.coverId,
                title = book.title,
                modifier = Modifier.fillMaxSize()
            )
            FavoriteIcon(
                isFavorite = book.isFavorite,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clickable(onClick = onFavoriteClick),
                size = 26.dp,
                activeColor = BookFavoriteColor,
                inactiveColor = Color.White
            )
            UserRatingBadge(
                rating = book.userRating,
                modifier = Modifier.padding(6.dp)
            )
        }

        MediaCardTitle(title = book.title)
        Spacer(modifier = Modifier.height(6.dp))
        ReadingProgressBar(
            progress = book.readingProgressPercent,
            compact = true
        )
    }
}

@Composable
private fun BookSearchHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun BookSearchResult(
    title: String,
    authors: String,
    firstPublishYear: Int?,
    coverId: Int?,
    onImageClick: (() -> Unit)? = null,
    onAddClick: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            BookCover(
                coverId = coverId,
                title = title,
                modifier = Modifier
                    .size(width = 88.dp, height = 132.dp)
                    .then(
                        if (onImageClick != null) {
                            Modifier.clickable(onClick = onImageClick)
                        } else {
                            Modifier
                        }
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (authors.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = authors,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                firstPublishYear?.let { year ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "First published: $year",
                        style = MaterialTheme.typography.bodySmall
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
private fun BookAddDialog(
    book: BookDto,
    onAdd: (BookStatus) -> Unit,
    onCancel: () -> Unit
) {
    var selectedStatus by remember(book.key) {
        mutableStateOf(BookStatus.TO_READ)
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add ${book.title}") },
        text = {
            BookStatusOptions(
                selectedStatus = selectedStatus,
                onStatusSelected = { status ->
                    selectedStatus = status
                }
            )
        },
        confirmButton = {
            Button(onClick = { onAdd(selectedStatus) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BookManageDialog(
    book: SavedBookEntity,
    onSave: (status: BookStatus, progress: Int) -> Unit,
    onChangeComment: () -> Unit,
    onRemove: () -> Unit,
    onCancel: () -> Unit
) {
    var selectedStatus by remember(book.key, book.status) {
        mutableStateOf(BookStatus.fromStorage(book.status))
    }
    var progress by remember(book.key, book.readingProgressPercent) {
        mutableStateOf(book.readingProgressPercent.coerceIn(0, 100))
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(book.title) },
        text = {
            Column {
                Text(
                    text = "Reading progress: $progress%",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                ReadingProgressBar(progress = progress)
                Slider(
                    value = progress.toFloat(),
                    onValueChange = { value ->
                        progress = value.roundToInt().coerceIn(0, 100)
                        selectedStatus = when {
                            progress == 100 -> BookStatus.FINISHED
                            selectedStatus == BookStatus.FINISHED ->
                                BookStatus.IN_PROGRESS
                            progress > 0 &&
                                selectedStatus == BookStatus.TO_READ ->
                                BookStatus.IN_PROGRESS
                            else -> selectedStatus
                        }
                    },
                    valueRange = 0f..100f,
                    steps = 99
                )

                if (selectedStatus == BookStatus.FINISHED) {
                    Text(
                        text = book.finishedAtEpochMillis?.let { timestamp ->
                            "Finished reading: ${formatStoredDate(timestamp)}"
                        } ?: "The finished-reading date will be set when saved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Category")
                BookStatusOptions(
                    selectedStatus = selectedStatus,
                    onStatusSelected = { status ->
                        selectedStatus = status
                        progress = when (status) {
                            BookStatus.FINISHED -> 100
                            BookStatus.TO_READ -> 0
                            BookStatus.IN_PROGRESS ->
                                progress.coerceIn(0, 99)
                            BookStatus.ABANDONED ->
                                progress.coerceIn(0, 99)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onChangeComment) {
                    Text(
                        if (book.note.isBlank()) {
                            "Add comment"
                        } else {
                            "Edit comment"
                        }
                    )
                }
                TextButton(onClick = onRemove) {
                    Text(
                        text = "Remove book",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedStatus, progress)
                }
            ) {
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

@Composable
private fun BookStatusOptions(
    selectedStatus: BookStatus,
    onStatusSelected: (BookStatus) -> Unit
) {
    Column {
        BookStatus.entries.forEach { status ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onStatusSelected(status)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = status == selectedStatus,
                    onClick = {
                        onStatusSelected(status)
                    }
                )
                Text(bookStatusLabel(status))
            }
        }
    }
}

@Composable
private fun BookDetailScreen(
    book: SavedBookEntity,
    onBackClick: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit,
    onSaveReadingState: (status: BookStatus, progress: Int) -> Unit,
    onChangeRating: (Int) -> Unit,
    onSaveComment: (String) -> Unit,
    onRemove: () -> Unit
) {
    BackHandler(onBack = onBackClick)
    var showManageDialog by remember(book.key) {
        mutableStateOf(false)
    }
    var showCommentDialog by remember(book.key) {
        mutableStateOf(false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            TextButton(onClick = onBackClick) {
                Text("← Back")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Book details",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    BookCover(
                        coverId = book.coverId,
                        title = book.title,
                        modifier = Modifier.size(width = 180.dp, height = 270.dp)
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (book.authors.isNotBlank()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = book.authors,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Reading progress: ${
                            book.readingProgressPercent.coerceIn(0, 100)
                        }%",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ReadingProgressBar(
                        progress = book.readingProgressPercent
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InlineUserRatingStars(
                        rating = book.userRating,
                        onRatingChange = onChangeRating,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    DetailLine(
                        label = "Category",
                        value = bookStatusLabel(
                            BookStatus.fromStorage(book.status)
                        )
                    )
                }
                book.firstPublishYear?.let { year ->
                    item {
                        DetailLine(
                            label = "First published",
                            value = year.toString()
                        )
                    }
                }
                item {
                    DetailLine(
                        label = "Editions",
                        value = book.editionCount.toString()
                    )
                }
                item {
                    DetailLine(
                        label = "Date added",
                        value = formatStoredDate(book.addedAtEpochMillis)
                    )
                }
                if (
                    book.readingProgressPercent >= 100 ||
                    BookStatus.fromStorage(book.status) == BookStatus.FINISHED
                ) {
                    item {
                        DetailLine(
                            label = "Finished reading",
                            value = formatStoredDate(book.finishedAtEpochMillis)
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider()
                    EditableMediaCommentSection(
                        mediaKey = book.key,
                        savedComment = book.note,
                        onSave = onSaveComment,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = 0.dp
                    )
                }
            }
        }

        TextButton(
            onClick = { onFavoriteClick(!book.isFavorite) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 116.dp, top = 20.dp)
        ) {
            FavoriteIcon(
                isFavorite = book.isFavorite,
                size = 30.dp,
                activeColor = BookFavoriteColor,
                inactiveColor = MaterialTheme.colorScheme.onSurface
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
        BookManageDialog(
            book = book,
            onSave = { status, progress ->
                onSaveReadingState(status, progress)
                showManageDialog = false
            },
            onChangeComment = {
                showManageDialog = false
                showCommentDialog = true
            },
            onRemove = {
                onRemove()
                showManageDialog = false
            },
            onCancel = {
                showManageDialog = false
            }
        )
    }

    if (showCommentDialog) {
        MediaCommentDialog(
            mediaKey = book.key,
            initialComment = book.note,
            onSave = { comment ->
                onSaveComment(comment)
                showCommentDialog = false
            },
            onCancel = {
                showCommentDialog = false
            }
        )
    }
}

@Composable
private fun ReadingProgressBar(
    progress: Int,
    compact: Boolean = false
) {
    val clampedProgress = progress.coerceIn(0, 100)
    val fraction = clampedProgress / 100f
    val progressColor = readingProgressColor(clampedProgress)

    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 5.dp else 12.dp),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        if (compact) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$clampedProgress%",
                style = MaterialTheme.typography.labelSmall,
                color = progressColor
            )
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun BookCover(
    coverId: Int?,
    title: String,
    modifier: Modifier = Modifier
) {
    val coverUrl = coverId?.let { id ->
        "$OPEN_LIBRARY_COVER_BASE_URL/$id-M.jpg?default=false"
    }

    if (coverUrl == null) {
        BookCoverPlaceholder(title = title, modifier = modifier)
        return
    }

    var isLoading by remember(coverUrl) {
        mutableStateOf(true)
    }
    var hasError by remember(coverUrl) {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceVariant
        ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(coverUrl)
                .crossfade(true)
                .build(),
            contentDescription = "$title cover",
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
                BookCoverPlaceholder(
                    title = title,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun BookCoverPlaceholder(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceVariant
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun normalizeProgress(
    status: BookStatus,
    progress: Int
): Int = when (status) {
    BookStatus.FINISHED -> 100
    BookStatus.TO_READ -> 0
    BookStatus.IN_PROGRESS,
    BookStatus.ABANDONED -> progress.coerceIn(0, 99)
}

private fun readingProgressColor(progress: Int): Color {
    val fraction = progress.coerceIn(0, 100) / 100f
    return Color(
        red = 1f - fraction,
        green = fraction,
        blue = 0f,
        alpha = 1f
    )
}

private fun bookStatusLabel(status: BookStatus): String = when (status) {
    BookStatus.IN_PROGRESS -> "En cours"
    BookStatus.FINISHED -> "Livres terminés"
    BookStatus.TO_READ -> "Livres à lire"
    BookStatus.ABANDONED -> "Livres abandonnés"
}
