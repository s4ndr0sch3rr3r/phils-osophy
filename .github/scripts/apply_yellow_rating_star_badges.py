from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


rating_components = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/UserRatingComponents.kt"
replace_once(
    rating_components,
    "import androidx.compose.foundation.background\n",
    "",
    "remove badge background import",
)
replace_once(
    rating_components,
    "import androidx.compose.foundation.layout.size\n",
    "",
    "remove badge size import",
)
replace_once(
    rating_components,
    "import androidx.compose.foundation.shape.CircleShape\n",
    "",
    "remove badge shape import",
)
replace_once(
    rating_components,
    "import androidx.compose.ui.draw.clip\n",
    "",
    "remove badge clip import",
)
replace_once(
    rating_components,
    "import androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.ui.unit.dp\n",
    "import androidx.compose.ui.semantics.contentDescription\nimport androidx.compose.ui.semantics.semantics\nimport androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.ui.unit.TextUnit\nimport androidx.compose.ui.unit.dp\n",
    "add rating star imports",
)
replace_once(
    rating_components,
    "private val UserRatingBadgeColor = Color(0xFFD32F2F)\n",
    "",
    "remove red badge color",
)
replace_once(
    rating_components,
    '''@Composable
fun BoxScope.UserRatingBadge(
    rating: Int,
    modifier: Modifier = Modifier
) {
    if (rating !in 1..USER_RATING_MAX) return

    Box(
        modifier = Modifier.align(Alignment.TopEnd),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(UserRatingBadgeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rating.toString(),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
''',
    '''@Composable
fun BoxScope.UserRatingBadge(
    rating: Int,
    modifier: Modifier = Modifier,
    starSize: TextUnit = 26.sp
) {
    if (rating !in 1..USER_RATING_MAX) return

    Text(
        text = "★",
        modifier = Modifier
            .align(Alignment.TopEnd)
            .then(modifier)
            .semantics {
                contentDescription =
                    "User rating: $rating out of $USER_RATING_MAX"
            },
        color = SelectedRatingStarColor,
        fontSize = starSize,
        fontWeight = FontWeight.Bold
    )
}
''',
    "replace numeric badge with yellow star",
)

movie_list = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/MovieListScreen.kt"
replace_once(
    movie_list,
    '''                UserRatingBadge(
                    rating = movie.userRating,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
''',
    '''                UserRatingBadge(
                    rating = movie.userRating,
                    modifier = Modifier.padding(6.dp)
                )
''',
    "movie rating star call",
)

series_library = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesLibraryScreen.kt"
replace_once(
    series_library,
    '''                UserRatingBadge(
                    rating = series.userRating,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
''',
    '''                UserRatingBadge(
                    rating = series.userRating,
                    modifier = Modifier.padding(6.dp),
                    starSize = 20.sp
                )
''',
    "series rating star call",
)

books_menu = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/BooksMenuScreen.kt"
replace_once(
    books_menu,
    '''            UserRatingBadge(
                rating = book.userRating,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            )
''',
    '''            UserRatingBadge(
                rating = book.userRating,
                modifier = Modifier.padding(6.dp)
            )
''',
    "book rating star call",
)
