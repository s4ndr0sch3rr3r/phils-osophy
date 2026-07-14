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


def add_import(path: Path, anchor: str) -> None:
    text = path.read_text(encoding="utf-8")
    favorite_import = "import com.example.phils_osophy.ui.components.FavoriteIcon\n"
    if favorite_import in text:
        return
    if anchor not in text:
        raise RuntimeError(f"Import anchor not found in {path}")
    path.write_text(
        text.replace(anchor, anchor + favorite_import, 1),
        encoding="utf-8",
    )


component_path = ROOT / "app/src/main/java/com/example/phils_osophy/ui/components/FavoriteIcon.kt"
component_path.parent.mkdir(parents=True, exist_ok=True)
component_path.write_text(
    '''package com.example.phils_osophy.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val FavoriteRed = Color(0xFFE53935)

@Composable
fun FavoriteIcon(
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    activeColor: Color = FavoriteRed,
    inactiveColor: Color = Color.White
) {
    Icon(
        imageVector = if (isFavorite) {
            FilledFavoriteIcon
        } else {
            OutlinedFavoriteIcon
        },
        contentDescription = if (isFavorite) {
            "Remove from favorites"
        } else {
            "Add to favorites"
        },
        modifier = modifier.size(size),
        tint = if (isFavorite) activeColor else inactiveColor
    )
}

private val FilledFavoriteIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "FilledFavorite",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 21.35f)
            lineTo(10.55f, 20.03f)
            cubicTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
            cubicTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
            cubicTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
            cubicTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
            cubicTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
            cubicTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.04f)
            lineTo(12f, 21.35f)
            close()
        }
    }.build()
}

private val OutlinedFavoriteIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "OutlinedFavorite",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(16.5f, 3f)
            cubicTo(14.76f, 3f, 13.09f, 3.81f, 12f, 5.09f)
            cubicTo(10.91f, 3.81f, 9.24f, 3f, 7.5f, 3f)
            cubicTo(4.42f, 3f, 2f, 5.42f, 2f, 8.5f)
            cubicTo(2f, 12.28f, 5.4f, 15.36f, 10.55f, 20.04f)
            lineTo(12f, 21.35f)
            lineTo(13.45f, 20.03f)
            cubicTo(18.6f, 15.36f, 22f, 12.28f, 22f, 8.5f)
            cubicTo(22f, 5.42f, 19.58f, 3f, 16.5f, 3f)
            close()

            moveTo(12.1f, 18.55f)
            lineTo(12f, 18.65f)
            lineTo(11.9f, 18.55f)
            cubicTo(7.14f, 14.24f, 4f, 11.39f, 4f, 8.5f)
            cubicTo(4f, 6.5f, 5.5f, 5f, 7.5f, 5f)
            cubicTo(9.04f, 5f, 10.54f, 5.99f, 11.07f, 7.36f)
            lineTo(12.93f, 7.36f)
            cubicTo(13.46f, 5.99f, 14.96f, 5f, 16.5f, 5f)
            cubicTo(18.5f, 5f, 20f, 6.5f, 20f, 8.5f)
            cubicTo(20f, 11.39f, 16.86f, 14.24f, 12.1f, 18.55f)
            close()
        }
    }.build()
}
''',
    encoding="utf-8",
)

movie_search = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/MovieSearchScreen.kt"
add_import(movie_search, "import com.example.phils_osophy.data.remote.TmdbClient\n")
replace_exact(
    movie_search,
    '''                Text(
                    text = if (showFavoritesOnly) "♥" else "♡",
                    color = if (showFavoritesOnly) {
                        FavoriteColor
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 30.sp
                )''',
    '''                FavoriteIcon(
                    isFavorite = showFavoritesOnly,
                    size = 30.dp,
                    activeColor = FavoriteColor,
                    inactiveColor = MaterialTheme.colorScheme.onSurface
                )''',
)

movie_list = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/MovieListScreen.kt"
add_import(movie_list, "import com.example.phils_osophy.data.local.SavedMovieEntity\n")
replace_exact(
    movie_list,
    '''                    Text(
                        text = if (movie.isFavorite) "♥" else "♡",
                        color = if (movie.isFavorite) {
                            FavoriteColor
                        } else {
                            Color.White
                        },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )''',
    '''                    FavoriteIcon(
                        isFavorite = movie.isFavorite,
                        size = 26.dp,
                        activeColor = FavoriteColor,
                        inactiveColor = Color.White
                    )''',
)

series_library = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesLibraryScreen.kt"
add_import(series_library, "import com.example.phils_osophy.data.remote.TmdbClient\n")
replace_exact(
    series_library,
    '''                Text(
                    text = if (showFavoritesOnly) "♥" else "♡",
                    color = if (showFavoritesOnly) {
                        LibraryFavoriteColor
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 30.sp
                )''',
    '''                FavoriteIcon(
                    isFavorite = showFavoritesOnly,
                    size = 30.dp,
                    activeColor = LibraryFavoriteColor,
                    inactiveColor = MaterialTheme.colorScheme.onSurface
                )''',
)
replace_exact(
    series_library,
    '''                Text(
                    text = if (series.isFavorite) "♥" else "♡",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(onClick = onFavoriteClick)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    color = if (series.isFavorite) {
                        LibraryFavoriteColor
                    } else {
                        Color.White
                    },
                    fontSize = 20.sp
                )''',
    '''                FavoriteIcon(
                    isFavorite = series.isFavorite,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(onClick = onFavoriteClick)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    size = 20.dp,
                    activeColor = LibraryFavoriteColor,
                    inactiveColor = Color.White
                )''',
)

series_detail = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesDetailScreen.kt"
add_import(series_detail, "import com.example.phils_osophy.data.remote.TmdbClient\n")
replace_exact(
    series_detail,
    '''            Text(
                text = if (isFavorite) "♥" else "♡",
                color = if (isFavorite) Color(0xFFE53935) else Color.White,
                fontSize = 30.sp
            )''',
    '''            FavoriteIcon(
                isFavorite = isFavorite,
                size = 30.dp,
                activeColor = Color(0xFFE53935),
                inactiveColor = Color.White
            )''',
)

books = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/BooksMenuScreen.kt"
add_import(books, "import com.example.phils_osophy.data.remote.OpenLibraryClient\n")
replace_exact(
    books,
    '''                Text(
                    text = if (showFavoritesOnly) "♥" else "♡",
                    color = if (showFavoritesOnly) {
                        BookFavoriteColor
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 30.sp
                )''',
    '''                FavoriteIcon(
                    isFavorite = showFavoritesOnly,
                    size = 30.dp,
                    activeColor = BookFavoriteColor,
                    inactiveColor = MaterialTheme.colorScheme.onSurface
                )''',
)
replace_exact(
    books,
    '''            Text(
                text = if (book.isFavorite) "♥" else "♡",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clickable(onClick = onFavoriteClick),
                color = if (book.isFavorite) {
                    BookFavoriteColor
                } else {
                    Color.White
                },
                fontSize = 26.sp
            )''',
    '''            FavoriteIcon(
                isFavorite = book.isFavorite,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clickable(onClick = onFavoriteClick),
                size = 26.dp,
                activeColor = BookFavoriteColor,
                inactiveColor = Color.White
            )''',
)
replace_exact(
    books,
    '''            Text(
                text = if (book.isFavorite) "♥" else "♡",
                color = if (book.isFavorite) {
                    BookFavoriteColor
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontSize = 30.sp
            )''',
    '''            FavoriteIcon(
                isFavorite = book.isFavorite,
                size = 30.dp,
                activeColor = BookFavoriteColor,
                inactiveColor = MaterialTheme.colorScheme.onSurface
            )''',
)

for path in [movie_search, movie_list, series_library, series_detail, books]:
    text = path.read_text(encoding="utf-8")
    if ".sp" not in text:
        text = text.replace("import androidx.compose.ui.unit.sp\n", "")
        path.write_text(text, encoding="utf-8")

remaining = []
for path in (ROOT / "app/src/main/java").rglob("*.kt"):
    text = path.read_text(encoding="utf-8")
    if "♥" in text or "♡" in text:
        remaining.append(str(path.relative_to(ROOT)))

if remaining:
    raise RuntimeError(f"Legacy favorite glyphs remain in: {remaining}")

print("Unified favorite icons applied successfully")
