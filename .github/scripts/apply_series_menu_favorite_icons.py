from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
path = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesMenuScreen.kt"
text = path.read_text(encoding="utf-8")

favorite_import = "import com.example.phils_osophy.ui.components.FavoriteIcon\n"
anchor = "import com.example.phils_osophy.data.remote.TmdbClient\n"
if favorite_import not in text:
    if anchor not in text:
        raise RuntimeError("Series menu import anchor not found")
    text = text.replace(anchor, anchor + favorite_import, 1)

replacements = [
    (
        '''                Text(
                    text = if (showFavoritesOnly) "♥" else "♡",
                    color = if (showFavoritesOnly) {
                        SeriesFavoriteColor
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 30.sp
                )''',
        '''                FavoriteIcon(
                    isFavorite = showFavoritesOnly,
                    size = 30.dp,
                    activeColor = SeriesFavoriteColor,
                    inactiveColor = MaterialTheme.colorScheme.onSurface
                )''',
    ),
    (
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
                        SeriesFavoriteColor
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
                    activeColor = SeriesFavoriteColor,
                    inactiveColor = Color.White
                )''',
    ),
]

for old, new in replacements:
    if old in text:
        text = text.replace(old, new, 1)
    elif new not in text:
        raise RuntimeError("Expected Series menu favorite block not found")

if ".sp" not in text:
    text = text.replace("import androidx.compose.ui.unit.sp\n", "")

path.write_text(text, encoding="utf-8")
print("Series menu favorite icons updated")
