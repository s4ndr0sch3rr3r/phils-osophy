package com.example.phils_osophy.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phils_osophy.BuildConfig
import com.example.phils_osophy.data.local.SavedGameEntity
import com.example.phils_osophy.data.remote.RawgClient
import com.example.phils_osophy.data.remote.RawgGameDetailsDto
import com.example.phils_osophy.data.remote.RawgGameSummaryDto
import com.example.phils_osophy.ui.util.formatStoredDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FinishedGreen = Color(0xFF2E7D32)
private val UnfinishedGrey = Color(0xFF9E9E9E)
private val HoursPattern = Regex(
    pattern = """^\s*(\d+):([0-5]\d)h?\s*$""",
    option = RegexOption.IGNORE_CASE
)
private val GameReleaseDateFormatter = DateTimeFormatter.ofPattern(
    "MMMM d, yyyy",
    Locale.ENGLISH
)

@Composable
fun GameLibraryScreen(
    games: List<SavedGameEntity>,
    onAddGame: (
        name: String,
        hoursPlayedMinutes: Int,
        playerCount: Int,
        isFinished: Boolean
    ) -> Unit,
    onBackClick: () -> Unit
) {
    var showAddDialog by remember {
        mutableStateOf(false)
    }
    var selectedGameId by remember {
        mutableStateOf<Long?>(null)
    }

    val apiKey = BuildConfig.RAWG_API_KEY.trim()
    val rawgSummaries = remember {
        mutableStateMapOf<Long, RawgGameSummaryDto?>()
    }

    LaunchedEffect(
        games.map { game -> game.id to game.name },
        apiKey
    ) {
        val activeGameIds = games.map { game -> game.id }.toSet()
        rawgSummaries.keys
            .filter { gameId -> gameId !in activeGameIds }
            .forEach { staleGameId ->
                rawgSummaries.remove(staleGameId)
            }

        if (apiKey.isBlank()) {
            rawgSummaries.clear()
            return@LaunchedEffect
        }

        games.forEach { game ->
            if (!rawgSummaries.containsKey(game.id)) {
                rawgSummaries[game.id] = try {
                    val results = RawgClient.api.searchGames(
                        apiKey = apiKey,
                        query = game.name
                    ).results

                    results.firstOrNull { result ->
                        result.name.equals(
                            game.name,
                            ignoreCase = true
                        )
                    } ?: results.firstOrNull()
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    val selectedGame = games.firstOrNull { game ->
        game.id == selectedGameId
    }

    if (selectedGameId != null) {
        if (selectedGame != null) {
            GameDetailContent(
                game = selectedGame,
                onBackClick = {
                    selectedGameId = null
                }
            )
        } else {
            EmptyPageScreen(
                title = "Game unavailable",
                onBackClick = {
                    selectedGameId = null
                }
            )
        }
        return
    }

    BackHandler(onBack = onBackClick)

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

        Text(
            text = "Games",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showAddDialog = true
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add a game",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "+",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "My games",
            style = MaterialTheme.typography.titleLarge
        )

        if (apiKey.isBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add RAWG_API_KEY to local.properties to load game images.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (games.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No games added yet.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(
                    items = games,
                    key = { game -> game.id }
                ) { game ->
                    GamePosterCard(
                        game = game,
                        imageUrl = rawgSummaries[game.id]
                            ?.backgroundImage,
                        onClick = {
                            selectedGameId = game.id
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddGameDialog(
            onAdd = {
                    name,
                    hoursPlayedMinutes,
                    playerCount,
                    isFinished ->
                onAddGame(
                    name,
                    hoursPlayedMinutes,
                    playerCount,
                    isFinished
                )
                showAddDialog = false
            },
            onCancel = {
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun GamePosterCard(
    game: SavedGameEntity,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.35f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl.isNullOrBlank()) {
                    Text(
                        text = game.name,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(
                            LocalContext.current
                        )
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${game.name} image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (game.isFinished) {
                                FinishedGreen
                            } else {
                                UnfinishedGrey
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatHours(game.hoursPlayedMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GameDetailContent(
    game: SavedGameEntity,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val apiKey = BuildConfig.RAWG_API_KEY.trim()
    var rawgDetails by remember(game.id) {
        mutableStateOf<RawgGameDetailsDto?>(null)
    }
    var isLoading by remember(game.id) {
        mutableStateOf(apiKey.isNotBlank())
    }
    var remoteError by remember(game.id) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(game.id, game.name, apiKey) {
        rawgDetails = null
        remoteError = null

        if (apiKey.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true

        try {
            val searchResults = RawgClient.api.searchGames(
                apiKey = apiKey,
                query = game.name
            ).results
            val match = searchResults.firstOrNull { result ->
                result.name.equals(
                    game.name,
                    ignoreCase = true
                )
            } ?: searchResults.firstOrNull()

            if (match == null) {
                remoteError =
                    "No matching game was found in RAWG."
            } else {
                rawgDetails = RawgClient.api.getGameDetails(
                    gameId = match.id,
                    apiKey = apiKey
                )
            }
        } catch (exception: Exception) {
            remoteError = exception.localizedMessage
                ?: "Game information could not be loaded."
        } finally {
            isLoading = false
        }
    }

    val uriHandler = LocalUriHandler.current
    val remoteTitle = rawgDetails
        ?.name
        ?.takeIf { name -> name.isNotBlank() }
        ?: game.name

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text("← Back")
            }

            Text(
                text = "Game details",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GameHero(
                    imageUrl = rawgDetails?.backgroundImage,
                    title = remoteTitle
                )
            }

            item {
                Text(
                    text = remoteTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                HorizontalDivider()
            }

            item {
                Text(
                    text = "My game",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                GameDetailRow(
                    label = "Status",
                    value = if (game.isFinished) {
                        "Finished"
                    } else {
                        "Not finished"
                    }
                )
            }

            item {
                GameDetailRow(
                    label = "Hours played",
                    value = formatHours(
                        game.hoursPlayedMinutes
                    )
                )
            }

            item {
                GameDetailRow(
                    label = "Game type",
                    value = playerLabel(game.playerCount)
                )
            }

            item {
                GameDetailRow(
                    label = "Date added",
                    value = formatStoredDate(
                        game.addedAtEpochMillis
                    )
                )
            }

            item {
                HorizontalDivider()
            }

            item {
                Text(
                    text = "Game information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            when {
                isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                apiKey.isBlank() -> {
                    item {
                        Text(
                            text =
                                "Add RAWG_API_KEY to local.properties " +
                                    "to load cover art, release " +
                                    "information, platforms, genres, " +
                                    "ratings and description.",
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                remoteError != null -> {
                    item {
                        Text(
                            text = remoteError.orEmpty(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                rawgDetails != null -> {
                    val details = rawgDetails!!

                    details.released
                        ?.takeIf { released ->
                            released.isNotBlank()
                        }
                        ?.let { released ->
                            item {
                                GameDetailRow(
                                    label = "Release date",
                                    value =
                                        formatGameReleaseDate(released)
                                )
                            }
                        }

                    joinedNames(
                        details.platforms.map { entry ->
                            entry.platform.name
                        }
                    )
                        .takeIf { value -> value.isNotBlank() }
                        ?.let { value ->
                            item {
                                GameDetailRow(
                                    label = "Platforms",
                                    value = value
                                )
                            }
                        }

                    joinedNames(
                        details.genres.map { genre ->
                            genre.name
                        }
                    )
                        .takeIf { value -> value.isNotBlank() }
                        ?.let { value ->
                            item {
                                GameDetailRow(
                                    label = "Genres",
                                    value = value
                                )
                            }
                        }

                    joinedNames(
                        details.developers.map { developer ->
                            developer.name
                        }
                    )
                        .takeIf { value -> value.isNotBlank() }
                        ?.let { value ->
                            item {
                                GameDetailRow(
                                    label = "Developer",
                                    value = value
                                )
                            }
                        }

                    joinedNames(
                        details.publishers.map { publisher ->
                            publisher.name
                        }
                    )
                        .takeIf { value -> value.isNotBlank() }
                        ?.let { value ->
                            item {
                                GameDetailRow(
                                    label = "Publisher",
                                    value = value
                                )
                            }
                        }

                    details.ageRating
                        ?.name
                        ?.takeIf { rating -> rating.isNotBlank() }
                        ?.let { rating ->
                            item {
                                GameDetailRow(
                                    label = "Age rating",
                                    value = rating
                                )
                            }
                        }

                    details.metacritic?.let { metacritic ->
                        item {
                            GameDetailRow(
                                label = "Metacritic",
                                value = metacritic.toString()
                            )
                        }
                    }

                    if (details.rating > 0.0) {
                        item {
                            GameDetailRow(
                                label = "RAWG rating",
                                value = java.lang.String.format(
                                    Locale.ENGLISH,
                                    "%.1f / 5",
                                    details.rating
                                )
                            )
                        }
                    }

                    if (details.playtime > 0) {
                        item {
                            GameDetailRow(
                                label = "Average playtime",
                                value = "${details.playtime} h"
                            )
                        }
                    }

                    if (details.description.isNotBlank()) {
                        item {
                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )
                            Text(
                                text = details.description,
                                style =
                                    MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    item {
                        TextButton(
                            onClick = {
                                uriHandler.openUri(
                                    "https://rawg.io"
                                )
                            }
                        ) {
                            Text("Game data provided by RAWG")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameHero(
    imageUrl: String?,
    title: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNullOrBlank()) {
            Text(
                text = title,
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(
                    LocalContext.current
                )
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$title image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun GameDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            modifier = Modifier.weight(1.4f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AddGameDialog(
    onAdd: (
        name: String,
        hoursPlayedMinutes: Int,
        playerCount: Int,
        isFinished: Boolean
    ) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }
    var hoursText by remember {
        mutableStateOf("00:00h")
    }
    var playerCount by remember {
        mutableStateOf(1)
    }
    var isFinished by remember {
        mutableStateOf(false)
    }

    val parsedHours = parseHours(hoursText)
    val canAdd = name.isNotBlank() && parsedHours != null

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text("Add game")
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Name")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Hours played")
                    },
                    supportingText = {
                        Text(
                            if (
                                hoursText.isNotBlank() &&
                                parsedHours == null
                            ) {
                                "Use the format 00:00h."
                            } else {
                                "Hours and minutes"
                            }
                        )
                    },
                    isError =
                        hoursText.isNotBlank() &&
                            parsedHours == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    )
                )

                Text(
                    text = "Number of people",
                    style = MaterialTheme.typography.labelLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = playerCount == 1,
                        onClick = {
                            playerCount = 1
                        },
                        label = {
                            Text("1 person")
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = playerCount == 2,
                        onClick = {
                            playerCount = 2
                        },
                        label = {
                            Text("2 people")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Finished",
                            style =
                                MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = if (isFinished) {
                                "Completed"
                            } else {
                                "Not completed"
                            },
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    FinishedTick(
                        isFinished = isFinished,
                        isInteractive = true,
                        onClick = {
                            isFinished = !isFinished
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(
                        name.trim(),
                        parsedHours ?: 0,
                        playerCount,
                        isFinished
                    )
                },
                enabled = canAdd
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun FinishedTick(
    isFinished: Boolean,
    isInteractive: Boolean,
    onClick: () -> Unit = {}
) {
    val clickModifier = if (isInteractive) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
                if (isFinished) {
                    FinishedGreen
                } else {
                    UnfinishedGrey
                }
            )
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✓",
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun parseHours(value: String): Int? {
    val match = HoursPattern.matchEntire(value)
        ?: return null
    val hours = match.groupValues[1].toIntOrNull()
        ?: return null
    val minutes = match.groupValues[2].toIntOrNull()
        ?: return null

    return hours * 60 + minutes
}

private fun formatHours(totalMinutes: Int): String {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60

    return "%02d:%02dh".format(hours, minutes)
}

private fun playerLabel(playerCount: Int): String =
    if (playerCount == 2) {
        "2 person game"
    } else {
        "1 person game"
    }

private fun joinedNames(values: List<String>): String =
    values
        .filter { value -> value.isNotBlank() }
        .distinct()
        .joinToString(", ")

private fun formatGameReleaseDate(value: String): String =
    runCatching {
        LocalDate.parse(value)
            .format(GameReleaseDateFormatter)
    }.getOrDefault(value)
