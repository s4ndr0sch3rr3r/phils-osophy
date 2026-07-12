from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    (ROOT / path).write_text(text, encoding="utf-8")


def replace_one(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def regex_one(text: str, pattern: str, replacement: str, label: str) -> str:
    result, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise RuntimeError(f"{label}: expected one regex match, found {count}")
    return result


# Shared UI.
write(
    "app/src/main/java/com/example/phils_osophy/ui/screens/UserRatingComponents.kt",
    '''package com.example.phils_osophy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val UserRatingBadgeColor = Color(0xFFD32F2F)
private val SelectedRatingStarColor = Color(0xFFFFC107)

@Composable
fun UserRatingBadge(
    rating: Int,
    modifier: Modifier = Modifier
) {
    if (rating !in 1..10) return

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

@Composable
fun UserRatingDialog(
    title: String,
    initialRating: Int,
    onSave: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var selectedRating by remember(title, initialRating) {
        mutableStateOf(initialRating.coerceIn(0, 10))
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..10).forEach { star ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable {
                                selectedRating = if (selectedRating == star) 0 else star
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "★",
                            color = if (star <= selectedRating) {
                                SelectedRatingStarColor
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontSize = 21.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedRating) }) {
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
'''
)

# Entities.
path = "app/src/main/java/com/example/phils_osophy/data/local/SavedSeriesEntity.kt"
t = read(path)
t = replace_one(t, '    @ColumnInfo(defaultValue = "0")\n    val isFavorite: Boolean\n)', '    @ColumnInfo(defaultValue = "0")\n    val isFavorite: Boolean,\n    @ColumnInfo(defaultValue = "0")\n    val userRating: Int\n)', "series entity field")
t = replace_one(t, '    isFavorite: Boolean = false\n): SavedSeriesEntity =', '    isFavorite: Boolean = false,\n    userRating: Int = 0\n): SavedSeriesEntity =', "series mapper param")
t = replace_one(t, '        status = status.name,\n        isFavorite = isFavorite\n', '        status = status.name,\n        isFavorite = isFavorite,\n        userRating = userRating.coerceIn(0, 10)\n', "series mapper value")
write(path, t)

path = "app/src/main/java/com/example/phils_osophy/data/local/SavedGameEntity.kt"
t = read(path)
t = replace_one(t, '    @ColumnInfo(defaultValue = "0")\n    val addedAtEpochMillis: Long = System.currentTimeMillis()\n)', '    @ColumnInfo(defaultValue = "0")\n    val addedAtEpochMillis: Long = System.currentTimeMillis(),\n    @ColumnInfo(defaultValue = "0")\n    val userRating: Int = 0\n)', "game entity field")
write(path, t)

path = "app/src/main/java/com/example/phils_osophy/data/local/SavedBookEntity.kt"
t = read(path)
t = replace_one(t, '    val readingProgressPercent: Int,\n    val finishedAtEpochMillis: Long?\n)', '    val readingProgressPercent: Int,\n    val finishedAtEpochMillis: Long?,\n    @ColumnInfo(defaultValue = "0")\n    val userRating: Int\n)', "book entity field")
t = replace_one(t, '    isFavorite: Boolean = false\n): SavedBookEntity {', '    isFavorite: Boolean = false,\n    userRating: Int = 0\n): SavedBookEntity {', "book mapper param")
t = replace_one(t, '        finishedAtEpochMillis = if (isFinished) {\n            System.currentTimeMillis()\n        } else {\n            null\n        }\n', '        finishedAtEpochMillis = if (isFinished) {\n            System.currentTimeMillis()\n        } else {\n            null\n        },\n        userRating = userRating.coerceIn(0, 10)\n', "book mapper value")
write(path, t)

# DAOs.
path = "app/src/main/java/com/example/phils_osophy/data/local/SavedSeriesDao.kt"
t = read(path)
t = replace_one(t, '    @Query("DELETE FROM saved_series WHERE id = :seriesId")', '''    @Query(
        "UPDATE saved_series SET userRating = :userRating WHERE id = :seriesId"
    )
    suspend fun updateRating(seriesId: Int, userRating: Int)

    @Query("DELETE FROM saved_series WHERE id = :seriesId")''', "series dao rating")
write(path, t)

path = "app/src/main/java/com/example/phils_osophy/data/local/SavedGameDao.kt"
t = read(path)
t = replace_one(t, '    @Insert(onConflict = OnConflictStrategy.REPLACE)\n    suspend fun insert(game: SavedGameEntity)\n', '''    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: SavedGameEntity)

    @Query(
        "UPDATE saved_games SET userRating = :userRating WHERE id = :gameId"
    )
    suspend fun updateRating(gameId: Long, userRating: Int)
''', "game dao rating")
write(path, t)

path = "app/src/main/java/com/example/phils_osophy/data/local/SavedBookDao.kt"
t = read(path)
t = replace_one(t, '    @Query("DELETE FROM saved_books WHERE `key` = :bookKey")', '''    @Query(
        "UPDATE saved_books SET userRating = :userRating WHERE `key` = :bookKey"
    )
    suspend fun updateRating(bookKey: String, userRating: Int)

    @Query("DELETE FROM saved_books WHERE `key` = :bookKey")''', "book dao rating")
write(path, t)

# Database migration.
path = "app/src/main/java/com/example/phils_osophy/data/local/PhilsOsophyDatabase.kt"
t = read(path)
t = replace_one(t, '    version = 10,', '    version = 11,', "database version")
t = replace_one(t, '        fun getInstance(context: Context): PhilsOsophyDatabase =', '''        private val migration10To11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE saved_series ADD COLUMN userRating " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE saved_games ADD COLUMN userRating " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE saved_books ADD COLUMN userRating " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): PhilsOsophyDatabase =''', "database migration declaration")
t = replace_one(t, '                        migration8To9,\n                        migration9To10\n', '                        migration8To9,\n                        migration9To10,\n                        migration10To11\n', "database migration registration")
write(path, t)

# App callbacks.
path = "app/src/main/java/com/example/phils_osophy/App.kt"
t = read(path)
t = replace_one(t, '''                        onFavoriteClick = { isFavorite ->
                            coroutineScope.launch {
                                savedSeriesDao.updateFavorite(
                                    seriesId = selectedSeries.id,
                                    isFavorite = isFavorite
                                )
                            }
                        },
                        onEpisodeClick =''', '''                        onFavoriteClick = { isFavorite ->
                            coroutineScope.launch {
                                savedSeriesDao.updateFavorite(
                                    seriesId = selectedSeries.id,
                                    isFavorite = isFavorite
                                )
                            }
                        },
                        onChangeRating = { rating ->
                            coroutineScope.launch {
                                savedSeriesDao.updateRating(
                                    seriesId = selectedSeries.id,
                                    userRating = rating.coerceIn(0, 10)
                                )
                            }
                        },
                        onEpisodeClick =''', "app series callback")
t = replace_one(t, '''                    },
                    onBackClick = ::openMovies
                )
            }

            AppScreen.BooksMenu''', '''                    },
                    onChangeRating = { gameId, rating ->
                        coroutineScope.launch {
                            savedGameDao.updateRating(
                                gameId = gameId,
                                userRating = rating.coerceIn(0, 10)
                            )
                        }
                    },
                    onBackClick = ::openMovies
                )
            }

            AppScreen.BooksMenu''', "app game callback")
write(path, t)

# Series library badge.
path = "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesLibraryScreen.kt"
t = read(path)
t = replace_one(t, '''                Text(
                    text = "•••",
                    modifier = Modifier
                        .align(Alignment.TopEnd)''', '''                UserRatingBadge(
                    rating = series.userRating,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                )

                Text(
                    text = "•••",
                    modifier = Modifier
                        .align(Alignment.TopEnd)''', "series library badge")
write(path, t)

# Series detail rating action.
path = "app/src/main/java/com/example/phils_osophy/ui/screens/SeriesDetailScreen.kt"
t = read(path)
t = replace_one(t, '    onFavoriteClick: (Boolean) -> Unit,\n    onEpisodeClick:', '    onFavoriteClick: (Boolean) -> Unit,\n    onChangeRating: (Int) -> Unit,\n    onEpisodeClick:', "series detail callback param")
t = replace_one(t, '    var loadingSeasonNumber by remember(series.id) {\n        mutableStateOf<Int?>(null)\n    }\n', '    var loadingSeasonNumber by remember(series.id) {\n        mutableStateOf<Int?>(null)\n    }\n    var isRatingDialogVisible by remember(series.id) {\n        mutableStateOf(false)\n    }\n', "series detail state")
t = replace_one(t, '''                    completedAtEpochMillis = seriesCompletedAtEpochMillis,
                    errorMessage = errorMessage
''', '''                    completedAtEpochMillis = seriesCompletedAtEpochMillis,
                    errorMessage = errorMessage,
                    onChangeRatingClick = { isRatingDialogVisible = true }
''', "series info callback")
t = replace_one(t, '''    }
}

@Composable
private fun SeriesHero''', '''    }

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
}

@Composable
private fun SeriesHero''', "series detail dialog")
t = replace_one(t, '    completedAtEpochMillis: Long?,\n    errorMessage: String?\n)', '    completedAtEpochMillis: Long?,\n    errorMessage: String?,\n    onChangeRatingClick: () -> Unit\n)', "series info param")
t = replace_one(t, '        item { HorizontalDivider() }\n\n        item {\n            DetailValue(\n                label = "First aired",', '''        item { HorizontalDivider() }

        item {
            DetailValue(
                label = "Your rating",
                value = if (series.userRating in 1..10) {
                    "${series.userRating} / 10"
                } else {
                    "Not rated"
                }
            )
            TextButton(onClick = onChangeRatingClick) {
                Text("Change rating")
            }
        }

        item {
            DetailValue(
                label = "First aired",''', "series info rating row")
write(path, t)

# Games rating UI.
path = "app/src/main/java/com/example/phils_osophy/ui/screens/GameLibraryScreen.kt"
t = read(path)
t = replace_one(t, '        isFinished: Boolean\n    ) -> Unit,\n    onBackClick: () -> Unit', '        isFinished: Boolean\n    ) -> Unit,\n    onChangeRating: (gameId: Long, rating: Int) -> Unit,\n    onBackClick: () -> Unit', "game screen callback")
t = replace_one(t, '''            GameDetailContent(
                game = selectedGame,
                onBackClick = {
                    selectedGameId = null
                }
            )''', '''            GameDetailContent(
                game = selectedGame,
                onBackClick = {
                    selectedGameId = null
                },
                onChangeRating = { rating ->
                    onChangeRating(selectedGame.id, rating)
                }
            )''', "game detail callback pass")
t = replace_one(t, '''                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)''', '''                UserRatingBadge(
                    rating = game.userRating,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)''', "game card badge")
t = replace_one(t, 'private fun GameDetailContent(\n    game: SavedGameEntity,\n    onBackClick: () -> Unit\n)', 'private fun GameDetailContent(\n    game: SavedGameEntity,\n    onBackClick: () -> Unit,\n    onChangeRating: (Int) -> Unit\n)', "game detail signature")
t = replace_one(t, '    var remoteError by remember(game.id) {\n        mutableStateOf<String?>(null)\n    }\n', '    var remoteError by remember(game.id) {\n        mutableStateOf<String?>(null)\n    }\n    var isRatingDialogVisible by remember(game.id) {\n        mutableStateOf(false)\n    }\n', "game detail rating state")
t = replace_one(t, '''            Text(
                text = "Game details",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
''', '''            Text(
                text = "Game details",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            TextButton(onClick = { isRatingDialogVisible = true }) {
                Text(if (game.userRating in 1..10) "${game.userRating}/10" else "Rate")
            }
''', "game detail rate button")
t = replace_one(t, '''    }
}

@Composable
private fun GameHero''', '''    }

    if (isRatingDialogVisible) {
        UserRatingDialog(
            title = "Rate ${game.name}",
            initialRating = game.userRating,
            onSave = { rating ->
                onChangeRating(rating)
                isRatingDialogVisible = false
            },
            onCancel = { isRatingDialogVisible = false }
        )
    }
}

@Composable
private fun GameHero''', "game detail dialog")
write(path, t)

# Books rating UI and DAO callback.
path = "app/src/main/java/com/example/phils_osophy/ui/screens/BooksMenuScreen.kt"
t = read(path)
t = replace_one(t, '''                onSaveReadingState = { status, progress ->
                    updateBook(selectedBook, status, progress)
                },
                onRemove =''', '''                onSaveReadingState = { status, progress ->
                    updateBook(selectedBook, status, progress)
                },
                onChangeRating = { rating ->
                    coroutineScope.launch {
                        savedBookDao.updateRating(
                            bookKey = selectedBook.key,
                            userRating = rating.coerceIn(0, 10)
                        )
                    }
                },
                onRemove =''', "book detail callback pass")
t = replace_one(t, '''            Text(
                text = "•••",
                modifier = Modifier
                    .align(Alignment.BottomEnd)''', '''            UserRatingBadge(
                rating = book.userRating,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            )
            Text(
                text = "•••",
                modifier = Modifier
                    .align(Alignment.BottomEnd)''', "book card badge")
t = replace_one(t, '    onSaveReadingState: (status: BookStatus, progress: Int) -> Unit,\n    onRemove: () -> Unit', '    onSaveReadingState: (status: BookStatus, progress: Int) -> Unit,\n    onChangeRating: (Int) -> Unit,\n    onRemove: () -> Unit', "book detail signature")
t = replace_one(t, '    var showManageDialog by remember {\n        mutableStateOf(false)\n    }\n', '    var showManageDialog by remember {\n        mutableStateOf(false)\n    }\n    var showRatingDialog by remember(book.key) {\n        mutableStateOf(false)\n    }\n', "book detail rating state")
t = replace_one(t, '''            TextButton(
                onClick = {
                    onFavoriteClick(!book.isFavorite)
                }
            ) {''', '''            TextButton(onClick = { showRatingDialog = true }) {
                Text(if (book.userRating in 1..10) "${book.userRating}/10" else "Rate")
            }
            TextButton(
                onClick = {
                    onFavoriteClick(!book.isFavorite)
                }
            ) {''', "book detail rate button")
t = regex_one(t, r'(private fun BookDetailScreen\([\s\S]*?\n}\n)(\n@Composable\nprivate fun BookCover)', r'''\1

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
}\2''', "book detail dialog")
# The regex above intentionally replaces the closing brace; fix duplicate structure if needed.
t = t.replace('\n}\n\n    if (showRatingDialog)', '\n\n    if (showRatingDialog)', 1)
write(path, t)

# Remove the one-shot automation files from the final branch.
(ROOT / ".github/workflows/apply-rating-system.yml").unlink(missing_ok=True)
Path(__file__).unlink(missing_ok=True)
