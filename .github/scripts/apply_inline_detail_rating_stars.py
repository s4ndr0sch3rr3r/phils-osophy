from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


components = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/UserRatingComponents.kt"
replace_once(
    components,
    'private val SelectedRatingStarColor = Color(0xFFFFC107)\n',
    'private val SelectedRatingStarColor = Color(0xFFFFC107)\n'
    'private val UnselectedRatingStarColor = Color(0xFF808080)\n',
    "add unselected rating color",
)
replace_once(
    components,
    '''@Composable
fun UserRatingDialog(
''',
    '''@Composable
fun InlineUserRatingStars(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedRating = rating.coerceIn(0, USER_RATING_MAX)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..USER_RATING_MAX).forEach { star ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clickable {
                        onRatingChange(
                            if (selectedRating == star) 0 else star
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "★",
                    color = if (star <= selectedRating) {
                        SelectedRatingStarColor
                    } else {
                        UnselectedRatingStarColor
                    },
                    fontSize = 38.sp
                )
            }
        }
    }
}

@Composable
fun UserRatingDialog(
''',
    "add inline rating component",
)

movie = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/MovieDetailScreen.kt"
replace_once(
    movie,
    '''    var isRatingDialogVisible by remember {
        mutableStateOf(false)
    }
''',
    '',
    "remove movie rating dialog state",
)
replace_once(
    movie,
    '''                    DropdownMenuItem(
                        text = {
                            Text("Change rating")
                        },
                        onClick = {
                            isMenuExpanded = false
                            isRatingDialogVisible = true
                        }
                    )

''',
    '',
    "remove movie detail rating menu item",
)
replace_once(
    movie,
    '''            Text(
                text = overview.ifBlank {
                    "No movie information available."
                },
                style = MaterialTheme.typography.bodyLarge
            )
''',
    '''            Text(
                text = overview.ifBlank {
                    "No movie information available."
                },
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(20.dp))

            InlineUserRatingStars(
                rating = movie.userRating,
                onRatingChange = onChangeRating
            )
''',
    "add movie inline stars",
)
replace_once(
    movie,
    '''
    if (isRatingDialogVisible) {
        ChangeMovieRatingDialog(
            initialRating = movie.userRating,
            onSave = { rating ->
                onChangeRating(rating)
                isRatingDialogVisible = false
            },
            onCancel = {
                isRatingDialogVisible = false
            }
        )
    }
''',
    '\n',
    "remove movie rating dialog",
)

series = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesDetailScreen.kt"
replace_once(
    series,
    '''    var isRatingDialogVisible by remember(series.id) {
        mutableStateOf(false)
    }
''',
    '',
    "remove series rating dialog state",
)
replace_once(
    series,
    '                    onChangeRatingClick = { isRatingDialogVisible = true },\n',
    '                    onRatingChange = onChangeRating,\n',
    "wire series inline rating callback",
)
replace_once(
    series,
    '''
    if (isRatingDialogVisible) {
        UserRatingDialog(
            title = "Rate ${series.name}",
            initialRating = series.userRating,
            onSave = { rating ->
                onChangeRating(rating)
                isRatingDialogVisible = false
            },
            onCancel = { isRatingDialogVisible = false }
        )
    }
''',
    '\n',
    "remove series rating dialog",
)
replace_once(
    series,
    '    onChangeRatingClick: () -> Unit,\n',
    '    onRatingChange: (Int) -> Unit,\n',
    "change series info rating callback",
)
replace_once(
    series,
    '''        item {
            Text(
                text = details
                    ?.overview
                    ?.takeIf { overview -> overview.isNotBlank() }
                    ?: series.overview.ifBlank {
                        "No series information available."
                    },
                style = MaterialTheme.typography.bodyLarge
            )
        }

        item { HorizontalDivider() }
''',
    '''        item {
            Text(
                text = details
                    ?.overview
                    ?.takeIf { overview -> overview.isNotBlank() }
                    ?: series.overview.ifBlank {
                        "No series information available."
                    },
                style = MaterialTheme.typography.bodyLarge
            )
        }

        item {
            InlineUserRatingStars(
                rating = series.userRating,
                onRatingChange = onRatingChange
            )
        }

        item { HorizontalDivider() }
''',
    "add series inline stars",
)
replace_once(
    series,
    '''        item {
            DetailValue(
                label = "Your rating",
                value = if (series.userRating in 1..USER_RATING_MAX) {
                    "${series.userRating} / $USER_RATING_MAX"
                } else {
                    "Not rated"
                }
            )
            TextButton(onClick = onChangeRatingClick) {
                Text("Change rating")
            }
        }

''',
    '',
    "remove series numeric rating block",
)

for path in (components, movie, series):
    text = path.read_text(encoding="utf-8")
    if "isRatingDialogVisible" in text and path.name in {"MovieDetailScreen.kt", "SeriesDetailScreen.kt"}:
        raise SystemExit(f"Unexpected detail rating dialog state remains in {path.name}")

print("Applied inline Movie and Series detail rating stars")
