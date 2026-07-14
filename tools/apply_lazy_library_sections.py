from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
BOOKS_PATH = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/BooksMenuScreen.kt"
SERIES_PATH = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesLibraryScreen.kt"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def replace_regex_once(text: str, pattern: str, replacement: str, label: str) -> str:
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.DOTALL)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one regex match, found {count}")
    return updated


books = BOOKS_PATH.read_text(encoding="utf-8")
books = replace_once(
    books,
    "import androidx.compose.foundation.lazy.LazyColumn\n",
    "import androidx.compose.foundation.lazy.LazyColumn\nimport androidx.compose.foundation.lazy.LazyListScope\n",
    "books LazyListScope import",
)
books = replace_once(
    books,
    "import androidx.compose.runtime.mutableStateOf\n",
    "import androidx.compose.runtime.mutableStateMapOf\nimport androidx.compose.runtime.mutableStateOf\n",
    "books mutableStateMapOf import",
)
books = replace_once(
    books,
    """    var pendingBook by remember { mutableStateOf<BookDto?>(null) }
    val coroutineScope = rememberCoroutineScope()
""",
    """    var pendingBook by remember { mutableStateOf<BookDto?>(null) }
    val expandedBookSections = remember {
        mutableStateMapOf<String, Boolean>()
    }
    val coroutineScope = rememberCoroutineScope()
""",
    "books expanded state",
)

old_books_sections = """            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    BookShelf(
                        title = \"En cours\",
                        books = filtered(inProgressBooks),
                        onBookClick = onBookClick,
                        onFavoriteClick = onFavoriteClick
                    )
                }
                item {
                    BookShelf(
                        title = \"Livres terminés\",
                        books = filtered(finishedBooks),
                        onBookClick = onBookClick,
                        onFavoriteClick = onFavoriteClick
                    )
                }
                item {
                    BookShelf(
                        title = \"Livres à lire\",
                        books = filtered(toReadBooks),
                        onBookClick = onBookClick,
                        onFavoriteClick = onFavoriteClick
                    )
                }
                item {
                    BookShelf(
                        title = \"Livres abandonnés\",
                        books = filtered(abandonedBooks),
                        onBookClick = onBookClick,
                        onFavoriteClick = onFavoriteClick
                    )
                }
            }
"""
new_books_sections = """            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                bookShelfItems(
                    title = \"En cours\",
                    books = filtered(inProgressBooks),
                    isExpanded = expandedBookSections[\"En cours\"] == true,
                    onToggleExpanded = {
                        expandedBookSections[\"En cours\"] =
                            expandedBookSections[\"En cours\"] != true
                    },
                    onBookClick = onBookClick,
                    onFavoriteClick = onFavoriteClick
                )
                bookShelfItems(
                    title = \"Livres terminés\",
                    books = filtered(finishedBooks),
                    isExpanded = expandedBookSections[\"Livres terminés\"] == true,
                    onToggleExpanded = {
                        expandedBookSections[\"Livres terminés\"] =
                            expandedBookSections[\"Livres terminés\"] != true
                    },
                    onBookClick = onBookClick,
                    onFavoriteClick = onFavoriteClick
                )
                bookShelfItems(
                    title = \"Livres à lire\",
                    books = filtered(toReadBooks),
                    isExpanded = expandedBookSections[\"Livres à lire\"] == true,
                    onToggleExpanded = {
                        expandedBookSections[\"Livres à lire\"] =
                            expandedBookSections[\"Livres à lire\"] != true
                    },
                    onBookClick = onBookClick,
                    onFavoriteClick = onFavoriteClick
                )
                bookShelfItems(
                    title = \"Livres abandonnés\",
                    books = filtered(abandonedBooks),
                    isExpanded = expandedBookSections[\"Livres abandonnés\"] == true,
                    onToggleExpanded = {
                        expandedBookSections[\"Livres abandonnés\"] =
                            expandedBookSections[\"Livres abandonnés\"] != true
                    },
                    onBookClick = onBookClick,
                    onFavoriteClick = onFavoriteClick
                )
            }
"""
books = replace_once(books, old_books_sections, new_books_sections, "books section list")

new_book_shelf = r'''private fun LazyListScope.bookShelfItems(
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
'''
books = replace_regex_once(
    books,
    r"@Composable\nprivate fun BookShelf\(.*?\n\}\n\n(?=@Composable\nprivate fun SavedBookCard)",
    new_book_shelf,
    "books BookShelf function",
)

if "private fun BookShelf(" in books:
    raise RuntimeError("legacy BookShelf function remains")
BOOKS_PATH.write_text(books, encoding="utf-8")


series = SERIES_PATH.read_text(encoding="utf-8")
series = replace_once(
    series,
    "import androidx.compose.foundation.lazy.LazyColumn\n",
    "import androidx.compose.foundation.lazy.LazyColumn\nimport androidx.compose.foundation.lazy.LazyListScope\n",
    "series LazyListScope import",
)
series = replace_once(
    series,
    "import androidx.compose.runtime.mutableStateOf\n",
    "import androidx.compose.runtime.mutableStateMapOf\nimport androidx.compose.runtime.mutableStateOf\n",
    "series mutableStateMapOf import",
)
series = replace_once(
    series,
    """    var pendingSeries by remember { mutableStateOf<SeriesDto?>(null) }
    val applicationContext = LocalContext.current.applicationContext
""",
    """    var pendingSeries by remember { mutableStateOf<SeriesDto?>(null) }
    val expandedSeriesSections = remember {
        mutableStateMapOf<String, Boolean>()
    }
    val applicationContext = LocalContext.current.applicationContext
""",
    "series expanded state",
)

old_series_sections = """            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    LibrarySection(
                        title = \"En cours\",
                        series = filtered(inProgressSeries),
                        onSeriesClick = onSeriesClick,
                        onFavoriteClick = onFavoriteClick
                    )
                }
                item {
                    LibrarySection(
                        title = \"Séries terminées\",
                        series = filtered(finishedSeries),
                        onSeriesClick = onSeriesClick,
                        onFavoriteClick = onFavoriteClick
                    )
                }
                item {
                    LibrarySection(
                        title = \"Séries à regarder\",
                        series = filtered(toWatchSeries),
                        onSeriesClick = onSeriesClick,
                        onFavoriteClick = onFavoriteClick
                    )
                }
                item {
                    LibrarySection(
                        title = \"Séries arrêtées\",
                        series = filtered(stoppedSeries),
                        onSeriesClick = onSeriesClick,
                        onFavoriteClick = onFavoriteClick
                    )
                }
            }
"""
new_series_sections = """            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                librarySectionItems(
                    title = \"En cours\",
                    series = filtered(inProgressSeries),
                    isExpanded = expandedSeriesSections[\"En cours\"] == true,
                    onToggleExpanded = {
                        expandedSeriesSections[\"En cours\"] =
                            expandedSeriesSections[\"En cours\"] != true
                    },
                    onSeriesClick = onSeriesClick,
                    onFavoriteClick = onFavoriteClick
                )
                librarySectionItems(
                    title = \"Séries terminées\",
                    series = filtered(finishedSeries),
                    isExpanded = expandedSeriesSections[\"Séries terminées\"] == true,
                    onToggleExpanded = {
                        expandedSeriesSections[\"Séries terminées\"] =
                            expandedSeriesSections[\"Séries terminées\"] != true
                    },
                    onSeriesClick = onSeriesClick,
                    onFavoriteClick = onFavoriteClick
                )
                librarySectionItems(
                    title = \"Séries à regarder\",
                    series = filtered(toWatchSeries),
                    isExpanded = expandedSeriesSections[\"Séries à regarder\"] == true,
                    onToggleExpanded = {
                        expandedSeriesSections[\"Séries à regarder\"] =
                            expandedSeriesSections[\"Séries à regarder\"] != true
                    },
                    onSeriesClick = onSeriesClick,
                    onFavoriteClick = onFavoriteClick
                )
                librarySectionItems(
                    title = \"Séries arrêtées\",
                    series = filtered(stoppedSeries),
                    isExpanded = expandedSeriesSections[\"Séries arrêtées\"] == true,
                    onToggleExpanded = {
                        expandedSeriesSections[\"Séries arrêtées\"] =
                            expandedSeriesSections[\"Séries arrêtées\"] != true
                    },
                    onSeriesClick = onSeriesClick,
                    onFavoriteClick = onFavoriteClick
                )
            }
"""
series = replace_once(series, old_series_sections, new_series_sections, "series section list")

new_series_section = r'''private fun LazyListScope.librarySectionItems(
    title: String,
    series: List<SavedSeriesEntity>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSeriesClick: (Int) -> Unit,
    onFavoriteClick: (
        seriesId: Int,
        isFavorite: Boolean
    ) -> Unit
) {
    val sectionKey = "series-section-$title"

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
        series.isEmpty() -> {
            item(
                key = "$sectionKey-empty",
                contentType = "library-section-empty"
            ) {
                Text(
                    text = "No series in this category.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        isExpanded -> {
            val rowCount = (series.size + 2) / 3
            items(
                count = rowCount,
                key = { rowIndex ->
                    "$sectionKey-row-${series[rowIndex * 3].id}"
                },
                contentType = { "series-grid-row" }
            ) { rowIndex ->
                val startIndex = rowIndex * 3
                val endIndex = minOf(startIndex + 3, series.size)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    for (seriesIndex in startIndex until endIndex) {
                        val savedSeries = series[seriesIndex]
                        LibrarySeriesPoster(
                            series = savedSeries,
                            onClick = {
                                onSeriesClick(savedSeries.id)
                            },
                            onFavoriteClick = {
                                onFavoriteClick(
                                    savedSeries.id,
                                    !savedSeries.isFavorite
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
                contentType = "series-horizontal-preview"
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    items(
                        items = series,
                        key = { savedSeries -> savedSeries.id },
                        contentType = { "series-card" }
                    ) { savedSeries ->
                        LibrarySeriesPoster(
                            series = savedSeries,
                            onClick = {
                                onSeriesClick(savedSeries.id)
                            },
                            onFavoriteClick = {
                                onFavoriteClick(
                                    savedSeries.id,
                                    !savedSeries.isFavorite
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
'''
series = replace_regex_once(
    series,
    r"@Composable\nprivate fun LibrarySection\(.*?\n\}\n\n(?=@Composable\nprivate fun LibrarySeriesPoster)",
    new_series_section,
    "series LibrarySection function",
)

if "private fun LibrarySection(" in series:
    raise RuntimeError("legacy LibrarySection function remains")
SERIES_PATH.write_text(series, encoding="utf-8")

print("Lazy expandable Book and Series sections applied")
