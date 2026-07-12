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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phils_osophy.data.local.BookStatus
import com.example.phils_osophy.data.local.PhilsOsophyDatabase
import com.example.phils_osophy.data.local.SavedBookEntity
import com.example.phils_osophy.data.local.toSavedBookEntity
import com.example.phils_osophy.data.remote.BookDto
import com.example.phils_osophy.data.remote.OpenLibraryClient
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    val coroutineScope = rememberCoroutineScope()
    var selectedBookKey by remember {
        mutableStateOf<String?>(null)
    }

    val selectedBook = allSavedBooks.firstOrNull { book ->
        book.key == selectedBookKey
    }

    if (selectedBookKey != null) {
        if (selectedBook != null) {
            BookDetailContent(
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
                onStatusChange = { status ->
                    coroutineScope.launch {
                        savedBookDao.updateStatus(
                            bookKey = selectedBook.key,
                            status = status.name
                        )
                    }
                },
                onRemove = {
                    coroutineScope.launch {
                        savedBookDao.deleteByKey(selectedBook.key)
                    }
                    selectedBookKey = null
                }
            )
        } else {
            EmptyPageScreen(
                title = "Book unavailable",
                onBackClick = {
                    selectedBookKey = null
                }
            )
        }
        return
    }

    BookLibraryContent(
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
        onStatusChange = { bookKey, status ->
            coroutineScope.launch {
                savedBookDao.updateStatus(
                    bookKey = bookKey,
                    status = status.name
                )
            }
        },
        onFavoriteClick = { bookKey, isFavorite ->
            coroutineScope.launch {
                savedBookDao.updateFavorite(
                    bookKey = bookKey,
                    isFavorite = isFavorite
                )
            }
        },
        onRemoveBook = { bookKey ->
            coroutineScope.launch {
                savedBookDao.deleteByKey(bookKey)
            }
        },
        onBackClick = onBackClick
    )
}

@Composable
private fun BookLibraryContent(
    inProgressBooks: List<SavedBookEntity>,
    finishedBooks: List<SavedBookEntity>,
    toReadBooks: List<SavedBookEntity>,
    abandonedBooks: List<SavedBookEntity>,
    onAddBook: (BookDto, BookStatus) -> Unit,
    onBookClick: (String) -> Unit,
    onStatusChange: (bookKey: String, status: BookStatus) -> Unit,
    onFavoriteClick: (bookKey: String, isFavorite: Boolean) -> Unit,
    onRemoveBook: (bookKey: String) -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    var query by remember { mutableStateOf("") }
    var searchResults by remember {
        mutableStateOf<List<BookDto>>(emptyList())
    }
    var isLoading by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var isSearchBarVisible by remember { mutableStateOf(true) }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }
    var pendingBook by remember {
        mutableStateOf<BookDto?>(null)
    }
    var managedBook by remember {
        mutableStateOf<SavedBookEntity?>(null)
    }

    val coroutineScope = rememberCoroutineScope()
    val searchBarScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                when {
                    available.y < -2f -> {
                        isSearchBarVisible = false
                    }

                    available.y > 2f -> {
                        isSearchBarVisible = true
                    }
                }

                return Offset.Zero
            }
        }
    }

    val allSavedBooks = (
        inProgressBooks +
            finishedBooks +
            toReadBooks +
            abandonedBooks
        ).distinctBy { book -> book.key }
    val savedBookKeys = allSavedBooks
        .map { book -> book.key }
        .toSet()

    fun resetSearch() {
        hasSearched = false
        searchResults = emptyList()
        errorMessage = null
        isLoading = false
    }

    fun filtered(
        books: List<SavedBookEntity>
    ): List<SavedBookEntity> = books.filter { savedBook ->
        !showFavoritesOnly || savedBook.isFavorite
    }

    fun searchBooks() {
        val cleanQuery = query.trim()

        if (cleanQuery.isBlank() || isLoading) {
            return
        }

        hasSearched = true
        errorMessage = null

        coroutineScope.launch {
            isLoading = true

            try {
                searchResults = OpenLibraryClient.api
                    .searchBooks(cleanQuery)
                    .docs
                    .filter { book ->
                        book.key.isNotBlank() &&
                            book.title.isNotBlank()
                    }
                    .distinctBy { book -> book.key }
            } catch (exception: Exception) {
                searchResults = emptyList()
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
        TextButton(onClick = onBackClick) {
            Text("← Back")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Books",
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
                Text(
                    text = if (showFavoritesOnly) "♥" else "♡",
                    color = if (showFavoritesOnly) {
                        BookFavoriteColor
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 30.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = isSearchBarVisible
        ) {
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
                            Text("Search for a book")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                searchBooks()
                            }
                        )
                    )

                    Button(
                        onClick = {
                            searchBooks()
                        },
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
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    BookLibrarySection(
                        title = "En cours",
                        books = filtered(inProgressBooks),
                        onBookClick = onBookClick,
                        onFavoriteClick = onFavoriteClick,
                        onManageClick = { book ->
                            managedBook = book
                        }
                    )
                }

                item {
                    BookLibrarySection(
                        title = "Livres terminés",
                        books = filtered(finishedBooks),
                        onBookClick = onBookClick,
                        onFavoriteClick = onFavoriteClick,
                        onManageClick = { book ->
                            managedBook = book
                        }
                    )
                }

                item {
                    BookLibrarySection(
                        title = "Livres à lire",
                        books = filtered(toReadBooks),
                        onBookClick = onBookClick,
                        onFavoriteClick = onFavoriteClick,
                        onManageClick = { book ->
                            managedBook = book
                        }
                    )
                }

                item {
                    BookLibrarySection(
                        title = "Livres abandonnés",
                        books = filtered(abandonedBooks),
                        onBookClick = onBookClick,
                        onFavoriteClick = onFavoriteClick,
                        onManageClick = { book ->
                            managedBook = book
                        }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    searchResults.isEmpty() -> {
                        Text("No books found.")
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = searchResults,
                                key = { book -> book.key }
                            ) { book ->
                                BookSearchResult(
                                    book = book,
                                    isAdded =
                                        book.key in savedBookKeys,
                                    onAddClick = {
                                        pendingBook = book
                                    }
                                )
                            }
                        }
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

    managedBook?.let { book ->
        BookManageDialog(
            book = book,
            onStatusChange = { status ->
                onStatusChange(book.key, status)
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
}

@Composable
private fun BookLibrarySection(
    title: String,
    books: List<SavedBookEntity>,
    onBookClick: (String) -> Unit,
    onFavoriteClick: (
        bookKey: String,
        isFavorite: Boolean
    ) -> Unit,
    onManageClick: (SavedBookEntity) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (books.isEmpty()) {
            Text(
                text = "No books in this category.",
                style = MaterialTheme.typography.bodyMedium
            )
            return
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(
                items = books,
                key = { book -> book.key }
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
                    },
                    onManageClick = {
                        onManageClick(book)
                    }
                )
            }
        }
    }
}

@Composable
private fun SavedBookCard(
    book: SavedBookEntity,
    onBookClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onManageClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(116.dp)
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = 116.dp,
                    height = 174.dp
                )
                .clickable(onClick = onBookClick)
        ) {
            BookCover(
                coverId = book.coverId,
                title = book.title,
                modifier = Modifier.fillMaxSize()
            )

            Text(
                text = if (book.isFavorite) "♥" else "♡",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clickable(onClick = onFavoriteClick),
                color = if (book.isFavorite) {
                    BookFavoriteColor
                } else {
                    Color.White
                },
                fontSize = 26.sp
            )

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
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = book.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BookSearchResult(
    book: BookDto,
    isAdded: Boolean,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            BookCover(
                coverId = book.coverId,
                title = book.title,
                modifier = Modifier.size(
                    width = 88.dp,
                    height = 132.dp
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (book.authorNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.authorNames.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                book.firstPublishYear?.let { year ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "First published: $year",
                        style = MaterialTheme.typography.bodySmall
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
        title = {
            Text("Add ${book.title}")
        },
        text = {
            BookStatusOptions(
                selectedStatus = selectedStatus,
                onStatusSelected = {
                    selectedStatus = it
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(selectedStatus)
                }
            ) {
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
    onStatusChange: (BookStatus) -> Unit,
    onRemove: () -> Unit,
    onCancel: () -> Unit
) {
    var selectedStatus by remember(book.key, book.status) {
        mutableStateOf(
            BookStatus.fromStorage(book.status)
        )
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(book.title)
        },
        text = {
            Column {
                Text("Move to")
                Spacer(modifier = Modifier.height(8.dp))
                BookStatusOptions(
                    selectedStatus = selectedStatus,
                    onStatusSelected = {
                        selectedStatus = it
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                    onStatusChange(selectedStatus)
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
private fun BookDetailContent(
    book: SavedBookEntity,
    onBackClick: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit,
    onStatusChange: (BookStatus) -> Unit,
    onRemove: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    var isManageDialogVisible by remember {
        mutableStateOf(false)
    }

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

            TextButton(
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
                    modifier = Modifier.size(
                        width = 180.dp,
                        height = 270.dp
                    )
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
                    value = formatAddedDate(
                        book.addedAtEpochMillis
                    )
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        isManageDialogVisible = true
                    }
                ) {
                    Text("Manage book")
                }
            }
        }
    }

    if (isManageDialogVisible) {
        BookManageDialog(
            book = book,
            onStatusChange = { status ->
                onStatusChange(status)
                isManageDialogVisible = false
            },
            onRemove = {
                onRemove()
                isManageDialogVisible = false
            },
            onCancel = {
                isManageDialogVisible = false
            }
        )
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
        BookCoverPlaceholder(
            title = title,
            modifier = modifier
        )
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
            model = ImageRequest.Builder(
                LocalContext.current
            )
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

private fun bookStatusLabel(status: BookStatus): String = when (status) {
    BookStatus.IN_PROGRESS -> "En cours"
    BookStatus.FINISHED -> "Livre terminé"
    BookStatus.TO_READ -> "Livre à lire"
    BookStatus.ABANDONED -> "Livre abandonné"
}

private fun formatAddedDate(epochMillis: Long): String {
    if (epochMillis <= 0L) {
        return "Unknown"
    }

    return Instant
        .ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(
            DateTimeFormatter.ofPattern("dd MMM yyyy")
        )
}