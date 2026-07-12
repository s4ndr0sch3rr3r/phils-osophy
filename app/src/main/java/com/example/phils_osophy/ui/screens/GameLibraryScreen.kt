package com.example.phils_osophy.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phils_osophy.data.local.SavedGameEntity

private val FinishedGreen = Color(0xFF2E7D32)
private val UnfinishedGrey = Color(0xFF9E9E9E)
private val HoursPattern = Regex(
    pattern = """^\s*(\d+):([0-5]\d)h?\s*$""",
    option = RegexOption.IGNORE_CASE
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
    var expandedGameId by remember {
        mutableStateOf<Long?>(null)
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
                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = games,
                    key = { game -> game.id }
                ) { game ->
                    GameListItem(
                        game = game,
                        isExpanded = game.id == expandedGameId,
                        onClick = {
                            expandedGameId = if (expandedGameId == game.id) {
                                null
                            } else {
                                game.id
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddGameDialog(
            onAdd = { name, hoursPlayedMinutes, playerCount, isFinished ->
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
private fun GameListItem(
    game: SavedGameEntity,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FinishedTick(
                    isFinished = game.isFinished,
                    isInteractive = false
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = game.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "${formatHours(game.hoursPlayedMinutes)} · " +
                            playerLabel(game.playerCount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = if (isExpanded) "⌃" else "⌄",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                GameDetailRow(
                    label = "Name",
                    value = game.name
                )
                GameDetailRow(
                    label = "Hours played",
                    value = formatHours(game.hoursPlayedMinutes)
                )
                GameDetailRow(
                    label = "Game type",
                    value = playerLabel(game.playerCount)
                )
                GameDetailRow(
                    label = "Status",
                    value = if (game.isFinished) {
                        "Finished"
                    } else {
                        "Not finished"
                    }
                )
            }
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
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            if (hoursText.isNotBlank() && parsedHours == null) {
                                "Use the format 00:00h."
                            } else {
                                "Hours and minutes"
                            }
                        )
                    },
                    isError = hoursText.isNotBlank() && parsedHours == null,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = if (isFinished) {
                                "Completed"
                            } else {
                                "Not completed"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val match = HoursPattern.matchEntire(value) ?: return null
    val hours = match.groupValues[1].toIntOrNull() ?: return null
    val minutes = match.groupValues[2].toIntOrNull() ?: return null

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
