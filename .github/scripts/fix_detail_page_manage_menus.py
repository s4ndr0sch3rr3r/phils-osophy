from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_once(text: str, old: str, new: str, path: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one match, found {count}")
    return text.replace(old, new, 1)


# Movie poster loading still needs LocalContext after card-management cleanup.
path = "app/src/main/java/com/example/phils_osophy/ui/screens/MovieListScreen.kt"
file = ROOT / path
text = file.read_text(encoding="utf-8")
anchor = "import androidx.compose.ui.layout.ContentScale\n"
if "import androidx.compose.ui.platform.LocalContext\n" not in text:
    text = replace_once(
        text,
        anchor,
        anchor + "import androidx.compose.ui.platform.LocalContext\n",
        path,
    )
file.write_text(text, encoding="utf-8")


# Rebuild Book detail with fixed top actions in the outer Box scope.
path = "app/src/main/java/com/example/phils_osophy/ui/screens/BooksMenuScreen.kt"
file = ROOT / path
text = file.read_text(encoding="utf-8")
start = text.index("@Composable\nprivate fun BookDetailScreen(")
end = text.index("\n@Composable\nprivate fun ReadingProgressBar(", start)
replacement = '''@Composable
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
    var showRatingDialog by remember(book.key) {
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
                TextButton(onClick = { showRatingDialog = true }) {
                    Text(
                        if (book.userRating in 1..USER_RATING_MAX) {
                            "${book.userRating}/$USER_RATING_MAX"
                        } else {
                            "Rate"
                        }
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
            onChangeRating = {
                showManageDialog = false
                showRatingDialog = true
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

    if (showRatingDialog) {
        UserRatingDialog(
            title = "Rate ${book.title}",
            initialRating = book.userRating,
            onSave = { rating ->
                onChangeRating(rating)
                showRatingDialog = false
            },
            onCancel = { showRatingDialog = false }
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
'''
text = text[:start] + replacement + text[end:]
file.write_text(text, encoding="utf-8")

print("Corrected detail menu layout")
