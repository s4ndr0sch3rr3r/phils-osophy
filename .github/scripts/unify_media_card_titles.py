from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one {label}; found {count}")
    return text.replace(old, new, 1)


movie_path = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/MovieListScreen.kt"
movie_text = movie_path.read_text(encoding="utf-8")
movie_start = movie_text.index("private fun MoviePosterTile(")
movie_manage = movie_text.index("\n    if (showManageDialog)", movie_start)
movie_section = movie_text[movie_start:movie_manage]
movie_section = replace_once(
    movie_section,
    "    Card(\n        onClick = onClick,\n        modifier = Modifier",
    "    Column(modifier = Modifier.fillMaxWidth()) {\n        Card(\n            onClick = onClick,\n            modifier = Modifier",
    "movie poster Card opening",
)
movie_text = (
    movie_text[:movie_start]
    + movie_section
    + "\n        MediaCardTitle(title = movie.title)\n    }\n"
    + movie_text[movie_manage:]
)
movie_path.write_text(movie_text, encoding="utf-8")


series_path = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesLibraryScreen.kt"
series_text = series_path.read_text(encoding="utf-8")
series_old = """            Text(
                text = series.name,
                modifier = Modifier.padding(8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )"""
series_text = replace_once(
    series_text,
    series_old,
    "            MediaCardTitle(title = series.name)",
    "series card title block",
)
series_path.write_text(series_text, encoding="utf-8")


books_path = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/BooksMenuScreen.kt"
books_text = books_path.read_text(encoding="utf-8")
books_old = """        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )"""
books_text = replace_once(
    books_text,
    books_old,
    "        MediaCardTitle(title = book.title)",
    "book card title block",
)
books_path.write_text(books_text, encoding="utf-8")
