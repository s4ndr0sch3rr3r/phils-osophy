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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phils_osophy.data.local.SavedSeriesEntity
import com.example.phils_osophy.data.local.SeriesStatus
import com.example.phils_osophy.data.remote.SeriesDto
import com.example.phils_osophy.data.remote.TmdbClient
import kotlinx.coroutines.launch

private const val TMDB_SERIES_POSTER_BASE_URL =
    "https://image.tmdb.org/t/p/w342"

private val SeriesFavoriteColor = Color(0xFFE53935)

@Composable
fun SeriesMenuScreen(
    inProgressSeries: List<SavedSeriesEntity>,
    finishedSeries: List<SavedSeriesEntity>,
    toWatchSeries: List<SavedSeriesEntity>,
    stoppedSeries: List<SavedSeriesEntity>,
    onSeriesClick: (Int) -> Unit,
    onAddSeries: (SeriesDto, SeriesStatus) -> Unit,
    onStatusChange: (seriesId: Int, status: SeriesStatus) -> Unit,
    onFavoriteClick: (seriesId: Int, isFavorite: Boolean) -> Unit,
    onRemoveSeries: (seriesId: Int) -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler {
        onBackClick()
    }

    var query by remember { mutableStateOf("") }
    var searchResults by remember {
        mutableStateOf<List<SeriesDto>>(emptyList())
    }
    var isLoading by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }
    var pendingSeries by remember {
        mutableStateOf<SeriesDto?>(null)
    }
    var managedSeries by remember {
        mutableStateOf<SavedSeriesEntity?>(null)
    }

    val allSavedSeries = (
        inProgressSeries +
            finishedSeries +
            toWatchSeries +
            stoppedSeries
        ).distinctBy { series -> series.id }
    val savedSeriesIds = allSavedSeries
        .map { series -> series.id }
        .toSet()

    fun visibleSeries(
        series: List<SavedSeriesEntity>
    ): List<SavedSeriesEntity> = series.filter { savedSeries ->
        val matchesFavorite =
            !showFavoritesOnly || savedSeries.isFavorite
        val matchesQuery =
            !hasSearched || savedSeries.name.contains(
                other = query.trim(),
                ignoreCase = true
            )

        matchesFavorite && matchesQuery
    }

    val coroutineScope = rememberCoroutineScope()

    val resetSearch = {
        hasSearched = false
        searchResults = emptyList()
        errorMessage = null
        isLoading = false
    }

    val searchSeries = {
        val cleanQuery = query.trim()

        if (cleanQuery.isNotEmpty() && !isLoading) {
            errorMessage = null
            hasSearched = true

            if (showFavoritesOnly) {
                searchResults = emptyList()
            } else {
                coroutineScope.launch {
                    isLoading = true

                    try {
                        searchResults = TmdbClient.api
                            .searchSeries(cleanQuery)
                            .results
                    } catch (exception: Exception) {
                        searchResults = emptyList()
                        errorMessage =
                            exception.localizedMessage
                                ?: "Series search failed."
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Series",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium
            )

            TextButton(
                onClick = {
                    showFavoritesOnly = !showFavoritesOnly
                    query = ""
                    resetSearch()
                }
            ) {
                Text(
                    text = if (showFavoritesOnly) "♥" else "♡",
                    color = if (showFavoritesOnly) {
                        SeriesFavoriteColor
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 30.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { newQuery ->
                    query = newQuery

                    if (newQuery.isBlank()) {
                        resetSearch()
                    }
                },
                modifier = Modifier.weight(1f),
                label = {
                    Text(
                        if (showFavoritesOnly) {
                            "Search within favorites"
                        } else {
                            "Search for a series"
                        }
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        searchSeries()
                    }
                )
            )

            Button(
                onClick = searchSeries,
                enabled = query.isNotBlank() && !isLoading
            ) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showFavoritesOnly || !hasSearched) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item(key = SeriesStatus.IN_PROGRESS.name) {
                    SeriesSection(
                        title = "En cours",
                        series = visibleSeries(inProgressSeries),
                        emptyMessage = emptySectionMessage(
                            showFavoritesOnly = showFavoritesOnly
                        ),
                        onSeriesClick = { series ->
                            onSeriesClick(series.id)
                        },
                        onManageClick = { series ->
                            managedSeries = series
                        },
                        onFavoriteClick = onFavoriteClick
                    )
                }

                item(key = SeriesStatus.FINISHED.name) {
                    SeriesSection(
                        title = "Séries terminées",
                        series = visibleSeries(finishedSeries),
                        emptyMessage = emptySectionMessage(
                            showFavoritesOnly = showFavoritesOnly
                        ),
                        onSeriesClick = { series ->
                            onSeriesClick(series.id)
                        },
                        onManageClick = { series ->
                            managedSeries = series
                        },
                        onFavoriteClick = onFavoriteClick
                    )
                }

                item(key = SeriesStatus.TO_WATCH.name) {
                    SeriesSection(
                        title = "Séries à regarder",
                        series = visibleSeries(toWatchSeries),
                        emptyMessage = emptySectionMessage(
                            showFavoritesOnly = showFavoritesOnly
                        ),
                        onSeriesClick = { series ->
                            onSeriesClick(series.id)
                        },
                        onManageClick = { series ->
                            managedSeries = series
                        },
                        onFavoriteClick = onFavoriteClick
                    )
                }

                item(key = SeriesStatus.STOPPED.name) {
                    SeriesSection(
                        title = "Séries arrêtées",
                        series = visibleSeries(stoppedSeries),
                        emptyMessage = emptySectionMessage(
                            showFavoritesOnly = showFavoritesOnly
                        ),
                        onSeriesClick = { series ->
                            onSeriesClick(series.id)
                        },
                        onManageClick = { series ->
                            managedSeries = series
                        },
                        onFavoriteClick = onFavoriteClick
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    searchResults.isEmpty() -> {
                        Text("No series found.")
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = searchResults,
                                key = { series -> series.id }
                            ) { series ->
                                SeriesResultCard(
                                    series = series,
                                    isAdded = series.id in savedSeriesIds,
                                    onAddClick = {
                                        pendingSeries = series
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingSeries?.let { series ->
        AddSeriesDialog(
            series = series,
            onAdd = { status ->
                onAddSeries(series, status)
                pendingSeries = null
            },
            onCancel = {
                pendingSeries = null
            }
        )
    }

    managedSeries?.let { series ->
        ManageSeriesDialog(
            series = series,
            onStatusChange = { status ->
                onStatusChange(series.id, status)
                managedSeries = null
            },
            onRemove = {
                onRemoveSeries(series.id)
                managedSeries = null
            },
            onCancel = {
                managedSeries = null
            }
        )
    }
}

private fun emptySectionMessage(
    showFavoritesOnly: Boolean
): String = if (showFavoritesOnly) {
    "No favorite series in this category."
} else {
    "No series in this category."
}

@Composable
private fun SeriesSection(
    title: String,
    series: List<SavedSeriesEntity>,
    emptyMessage: String,
    onSeriesClick: (SavedSeriesEntity) -> Unit,
    onManageClick: (SavedSeriesEntity) -> Unit,
    onFavoriteClick: (seriesId: Int, isFavorite: Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (series.isEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(
                    items = series,
                    key = { savedSeries -> savedSeries.id }
                ) { savedSeries ->
                    SavedSeriesPoster(
                        series = savedSeries,
                        onClick = {
                            onSeriesClick(savedSeries)
                        },
                        onManageClick = {
                            onManageClick(savedSeries)
                        },
                        onFavoriteClick = {
                            onFavoriteClick(
                                savedSeries.id,
                                !savedSeries.isFavorite
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedSeriesPoster(
    series: SavedSeriesEntity,
    onClick: () -> Unit,
    onManageClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val posterUrl = series.posterPath
        ?.takeIf { path -> path.isNotBlank() }
        ?.let { path ->
            "$TMDB_SERIES_POSTER_BASE_URL$path"
        }
    var isLoading by remember(posterUrl) {
        mutableStateOf(posterUrl != null)
    }
    var hasError by remember(posterUrl) {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 180.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (posterUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(
                            LocalContext.current
                        )
                            .data(posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${series.name} poster",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onLoading = {
                            isLoading = true
                            hasError = false
                        },
                        onSuccess = {
                            isLoading = false
                            hasError = false
                        },
                        onError = {
                            isLoading = false
                            hasError = true
                        }
                    )
                }

                if (posterUrl == null || isLoading || hasError) {
                    Text(
                        text = series.name,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
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
                )

                Text(
                    text = "•••",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(onClick = onManageClick)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = series.name,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SeriesResultCard(
    series: SeriesDto,
    isAdded: Boolean,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            SeriesSearchPoster(
                series = series,
                modifier = Modifier.size(
                    width = 96.dp,
                    height = 144.dp
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = series.name,
                    style = MaterialTheme.typography.titleMedium
                )

                if (series.firstAirDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "First aired: ${series.firstAirDate}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Rating: %.1f / 10".format(
                        series.voteAverage
                    ),
                    style = MaterialTheme.typography.bodySmall
                )

                if (series.overview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = series.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onAddClick,
                    enabled = !isAdded
                ) {
                    Text(
                        if (isAdded) {
                            "Added"
                        } else {
                            "+ Add"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesSearchPoster(
    series: SeriesDto,
    modifier: Modifier = Modifier
) {
    val posterUrl = series.posterPath
        ?.takeIf { path -> path.isNotBlank() }
        ?.let { path ->
            "$TMDB_SERIES_POSTER_BASE_URL$path"
        }

    if (posterUrl == null) {
        SeriesPosterPlaceholder(
            text = series.name,
            modifier = modifier
        )
        return
    }

    var isLoading by remember(posterUrl) {
        mutableStateOf(true)
    }
    var hasError by remember(posterUrl) {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceVariant
        ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(
                LocalContext.current
            )
                .data(posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = "${series.name} poster",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onLoading = {
                isLoading = true
                hasError = false
            },
            onSuccess = {
                isLoading = false
                hasError = false
            },
            onError = {
                isLoading = false
                hasError = true
            }
        )

        if (isLoading || hasError) {
            Text(
                text = series.name,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SeriesPosterPlaceholder(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceVariant
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AddSeriesDialog(
    series: SeriesDto,
    onAdd: (SeriesStatus) -> Unit,
    onCancel: () -> Unit
) {
    var selectedStatus by remember(series.id) {
        mutableStateOf(SeriesStatus.TO_WATCH)
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Add ${series.name}",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            StatusChoices(
                selectedStatus = selectedStatus,
                onStatusSelected = { status ->
                    selectedStatus = status
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(selectedStatus)
                }
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
private fun ManageSeriesDialog(
    series: SavedSeriesEntity,
    onStatusChange: (SeriesStatus) -> Unit,
    onRemove: () -> Unit,
    onCancel: () -> Unit
) {
    var selectedStatus by remember(series.id, series.status) {
        mutableStateOf(
            SeriesStatus.fromStorage(series.status)
        )
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = series.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                Text("Move to")

                Spacer(modifier = Modifier.height(8.dp))

                StatusChoices(
                    selectedStatus = selectedStatus,
                    onStatusSelected = { status ->
                        selectedStatus = status
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onRemove) {
                    Text(
                        text = "Remove series",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onStatusChange(selectedStatus)
                }
            ) {
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

@Composable
private fun StatusChoices(
    selectedStatus: SeriesStatus,
    onStatusSelected: (SeriesStatus) -> Unit
) {
    Column {
        SeriesStatus.values().forEach { status ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onStatusSelected(status)
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = status == selectedStatus,
                    onClick = {
                        onStatusSelected(status)
                    }
                )

                Text(status.displayName())
            }
        }
    }
}

private fun SeriesStatus.displayName(): String = when (this) {
    SeriesStatus.IN_PROGRESS -> "En cours"
    SeriesStatus.FINISHED -> "Séries terminées"
    SeriesStatus.TO_WATCH -> "Séries à regarder"
    SeriesStatus.STOPPED -> "Séries arrêtées"
}
