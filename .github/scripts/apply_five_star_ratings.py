from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    (ROOT / path).write_text(text, encoding="utf-8")


def replace_exact(text: str, old: str, new: str, expected: int, label: str) -> str:
    actual = text.count(old)
    if actual != expected:
        raise SystemExit(f"{label}: expected {expected} occurrence(s), found {actual}")
    return text.replace(old, new)


rating_components = "app/src/main/java/com/example/phils_osophy/ui/screens/UserRatingComponents.kt"
text = read(rating_components)
text = replace_exact(
    text,
    "private val SelectedRatingStarColor = Color(0xFFFFC107)\n",
    "private val SelectedRatingStarColor = Color(0xFFFFC107)\n\nconst val USER_RATING_MAX = 5\n",
    1,
    "rating max constant",
)
text = replace_exact(text, "rating !in 1..10", "rating !in 1..USER_RATING_MAX", 1, "badge range")
text = replace_exact(
    text,
    "initialRating.coerceIn(0, 10)",
    "initialRating.coerceIn(0, USER_RATING_MAX)",
    1,
    "shared dialog clamp",
)
text = replace_exact(text, "(1..10).forEach", "(1..USER_RATING_MAX).forEach", 1, "shared dialog stars")
write(rating_components, text)

add_movie = "app/src/main/java/com/example/phils_osophy/ui/screens/AddMovieDialog.kt"
text = read(add_movie)
text = replace_exact(
    text,
    'text = "Your rating: $selectedRating / 10"',
    'text = "Your rating: $selectedRating / $USER_RATING_MAX"',
    1,
    "add movie rating label",
)
text = replace_exact(text, "(1..10).forEach", "(1..USER_RATING_MAX).forEach", 1, "add movie stars")
write(add_movie, text)

change_movie = "app/src/main/java/com/example/phils_osophy/ui/screens/ChangeMovieRatingDialog.kt"
text = read(change_movie)
text = replace_exact(
    text,
    "initialRating.coerceIn(0, 10)",
    "initialRating.coerceIn(0, USER_RATING_MAX)",
    1,
    "movie rating dialog clamp",
)
text = replace_exact(
    text,
    'text = "Your rating: $selectedRating / 10"',
    'text = "Your rating: $selectedRating / $USER_RATING_MAX"',
    1,
    "movie rating dialog label",
)
text = replace_exact(text, "(1..10).forEach", "(1..USER_RATING_MAX).forEach", 1, "movie rating dialog stars")
write(change_movie, text)

app = "app/src/main/java/com/example/phils_osophy/App.kt"
text = read(app)
text = replace_exact(
    text,
    "import com.example.phils_osophy.ui.screens.SeriesLibraryScreen\n",
    "import com.example.phils_osophy.ui.screens.SeriesLibraryScreen\nimport com.example.phils_osophy.ui.screens.USER_RATING_MAX\n",
    1,
    "App rating import",
)
text = replace_exact(
    text,
    "rating.coerceIn(0, 10)",
    "rating.coerceIn(0, USER_RATING_MAX)",
    6,
    "App rating clamps",
)
write(app, text)

movie_list = "app/src/main/java/com/example/phils_osophy/ui/screens/MovieListScreen.kt"
text = read(movie_list)
text = replace_exact(text, "movie.userRating in 1..10", "movie.userRating in 1..USER_RATING_MAX", 2, "movie menu ranges")
text = replace_exact(
    text,
    '"Your rating: ${movie.userRating} / 10"',
    '"Your rating: ${movie.userRating} / $USER_RATING_MAX"',
    1,
    "movie menu label",
)
write(movie_list, text)

series_library = "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesLibraryScreen.kt"
text = read(series_library)
text = replace_exact(
    text,
    "series.userRating in 1..10",
    "series.userRating in 1..USER_RATING_MAX",
    1,
    "series menu range",
)
write(series_library, text)

series_detail = "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesDetailScreen.kt"
text = read(series_detail)
text = replace_exact(
    text,
    "series.userRating in 1..10",
    "series.userRating in 1..USER_RATING_MAX",
    1,
    "series detail range",
)
text = replace_exact(
    text,
    '"${series.userRating} / 10"',
    '"${series.userRating} / $USER_RATING_MAX"',
    1,
    "series detail user rating",
)
write(series_detail, text)

games = "app/src/main/java/com/example/phils_osophy/ui/screens/GameLibraryScreen.kt"
text = read(games)
text = replace_exact(
    text,
    "game.userRating in 1..10",
    "game.userRating in 1..USER_RATING_MAX",
    1,
    "game detail range",
)
text = replace_exact(
    text,
    '"${game.userRating}/10"',
    '"${game.userRating}/$USER_RATING_MAX"',
    1,
    "game detail rating label",
)
write(games, text)

books = "app/src/main/java/com/example/phils_osophy/ui/screens/BooksMenuScreen.kt"
text = read(books)
text = replace_exact(
    text,
    "rating.coerceIn(0, 10)",
    "rating.coerceIn(0, USER_RATING_MAX)",
    2,
    "book rating clamps",
)
text = replace_exact(
    text,
    "book.userRating in 1..10",
    "book.userRating in 1..USER_RATING_MAX",
    2,
    "book rating ranges",
)
text = replace_exact(
    text,
    '"${book.userRating}/10"',
    '"${book.userRating}/$USER_RATING_MAX"',
    1,
    "book detail rating label",
)
write(books, text)

database = "app/src/main/java/com/example/phils_osophy/data/local/PhilsOsophyDatabase.kt"
text = read(database)
text = replace_exact(text, "version = 12,", "version = 13,", 1, "database version")
marker = '''        private val migration11To12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE saved_series ADD COLUMN note " +
                        "TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE saved_books ADD COLUMN note " +
                        "TEXT NOT NULL DEFAULT ''"
                )
            }
        }
'''
replacement = marker + '''
        private val migration12To13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                listOf(
                    "saved_movies",
                    "saved_series",
                    "saved_games",
                    "saved_books"
                ).forEach { tableName ->
                    database.execSQL(
                        "UPDATE $tableName SET userRating = CASE " +
                            "WHEN userRating <= 0 THEN 0 " +
                            "WHEN userRating <= 2 THEN 1 " +
                            "WHEN userRating <= 4 THEN 2 " +
                            "WHEN userRating <= 6 THEN 3 " +
                            "WHEN userRating <= 8 THEN 4 " +
                            "ELSE 5 END"
                    )
                }
            }
        }
'''
text = replace_exact(text, marker, replacement, 1, "rating data migration")
text = replace_exact(
    text,
    "                        migration11To12\n",
    "                        migration11To12,\n                        migration12To13\n",
    1,
    "migration registration",
)
write(database, text)

forbidden = (
    "userRating in 1..10",
    "rating !in 1..10",
    "rating.coerceIn(0, 10)",
    "initialRating.coerceIn(0, 10)",
    "(1..10).forEach",
    "$selectedRating / 10",
    "userRating}/10",
    "userRating} / 10",
)
violations = []
source_root = ROOT / "app/src/main/java"
for path in source_root.rglob("*.kt"):
    source = path.read_text(encoding="utf-8")
    for pattern in forbidden:
        if pattern in source:
            violations.append(f"{path.relative_to(ROOT)}: {pattern}")
if violations:
    raise SystemExit("Old 10-point user-rating patterns remain:\n" + "\n".join(violations))
