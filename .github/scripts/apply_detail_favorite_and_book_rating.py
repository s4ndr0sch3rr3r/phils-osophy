from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_exact(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old in text:
        path.write_text(text.replace(old, new, 1), encoding="utf-8")
        return
    if new in text:
        return
    raise RuntimeError(f"Expected source block not found in {path}")


movie_path = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/MovieDetailScreen.kt"
book_path = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/BooksMenuScreen.kt"

replace_exact(
    movie_path,
    "import com.example.phils_osophy.data.remote.TmdbClient\n",
    "import com.example.phils_osophy.data.remote.TmdbClient\n"
    "import com.example.phils_osophy.ui.components.FavoriteIcon\n",
)

replace_exact(
    movie_path,
    '''            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 66.dp, top = 20.dp)
            ) {''',
    '''            TextButton(
                onClick = { onFavoriteClick(!movie.isFavorite) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 116.dp, top = 20.dp)
            ) {
                FavoriteIcon(
                    isFavorite = movie.isFavorite,
                    size = 30.dp,
                    activeColor = Color(0xFFE53935),
                    inactiveColor = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 66.dp, top = 20.dp)
            ) {''',
)

replace_exact(
    movie_path,
    '''                    DropdownMenuItem(
                        text = {
                            Text(
                                if (movie.isFavorite) {
                                    "Remove favorite"
                                } else {
                                    "Favorite"
                                }
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onFavoriteClick(!movie.isFavorite)
                        }
                    )

''',
    "",
)

replace_exact(
    book_path,
    '''    onSave: (status: BookStatus, progress: Int) -> Unit,
    onChangeRating: () -> Unit,
    onChangeComment: () -> Unit,''',
    '''    onSave: (status: BookStatus, progress: Int) -> Unit,
    onChangeComment: () -> Unit,''',
)

replace_exact(
    book_path,
    '''                TextButton(onClick = onChangeRating) {
                    Text(
                        if (book.userRating in 1..USER_RATING_MAX) {
                            "Change rating"
                        } else {
                            "Add rating"
                        }
                    )
                }
''',
    "",
)

replace_exact(
    book_path,
    '''    var showRatingDialog by remember(book.key) {
        mutableStateOf(false)
    }
''',
    "",
)

replace_exact(
    book_path,
    '''                TextButton(onClick = { showRatingDialog = true }) {
                    Text(
                        if (book.userRating in 1..USER_RATING_MAX) {
                            "${book.userRating}/$USER_RATING_MAX"
                        } else {
                            "Rate"
                        }
                    )
                }
''',
    "",
)

replace_exact(
    book_path,
    '''                    ReadingProgressBar(
                        progress = book.readingProgressPercent
                    )
''',
    '''                    ReadingProgressBar(
                        progress = book.readingProgressPercent
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InlineUserRatingStars(
                        rating = book.userRating,
                        onRatingChange = onChangeRating,
                        modifier = Modifier.fillMaxWidth()
                    )
''',
)

replace_exact(
    book_path,
    '''            onChangeRating = {
                showManageDialog = false
                showRatingDialog = true
            },
''',
    "",
)

replace_exact(
    book_path,
    '''    if (showRatingDialog) {
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

''',
    "",
)

movie_text = movie_path.read_text(encoding="utf-8")
book_text = book_path.read_text(encoding="utf-8")

checks = {
    "Movie favorite icon": "FavoriteIcon(\n                    isFavorite = movie.isFavorite" in movie_text,
    "Movie duplicate favorite menu action removed": "Remove favorite" not in movie_text,
    "Book inline rating stars": "InlineUserRatingStars(\n                        rating = book.userRating" in book_text,
    "Book Rate button removed": '"Rate"' not in book_text[book_text.index("private fun BookDetailScreen"):],
    "Book rating dialog state removed": "showRatingDialog" not in book_text,
    "Book management rating action removed": "onChangeRating: () -> Unit" not in book_text,
}

failed = [name for name, passed in checks.items() if not passed]
if failed:
    raise RuntimeError("Validation failed: " + ", ".join(failed))

print("Detail favorite and book inline rating updates applied")
