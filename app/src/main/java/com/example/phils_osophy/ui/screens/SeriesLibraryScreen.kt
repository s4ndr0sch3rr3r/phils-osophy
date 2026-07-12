package com.example.phils_osophy.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.example.phils_osophy.data.local.SeriesCompletionTracker
import com.example.phils_osophy.data.local.SeriesStatus
import com.example.phils_osophy.data.remote.SeriesDto
import com.example.phils_osophy.data.remote.TmdbClient
import kotlinx.coroutines.launch

private const val TMDB_LIBRARY_POSTER_BASE_URL =
    "https://image.tmdb.org/t/p/w342"

private val LibraryFavoriteColor = Color(0xFFE53935)

@Composable
fun SeriesLibraryScreen(
    inProgressSeries: List<SavedSeriesEntity>,
    finishedSeries: List<SavedSeriesEntity>,
    toWatchSeries: List<SavedSeriesEntity>,
    stoppedSeries: List<SavedSeriesEntity>,
    onAddSeries: (SeriesDto, SeriesStatus) -> Unit,
    onSeriesClick: (Int) -> Unit,
    onStatusChange: (seriesId: Int, status: SeriesStatus) -> Unit,
    onFavoriteClick: (seriesId: Int, isFavorite: Boolean) -> Unit,
    onChangeRating: (seriesId: Int, rating: Int) -> Unit,
    onRemoveSeries: (seriesId: Int) -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    var query by remember { mutableStateOf("") }
    var searchResults by remember {
        mutableStateOf<List<SeriesDto>>(emptyList())
    }
    var isLoading by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var isSearchBarVisible by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingSeries by remember { mutableStateOf<SeriesDto?>(null) }
    var managedSeries by remember {
        mutableStateOf<SavedSeriesEntity?>(null)
    }
    var ratingSeries by remember {
        mutableStateOf<SavedSeriesEntity?>(null)
    }

    val applicationContext = LocalContext.current.applicationContext
    val completionTracker = remember(applicationContext) {
        SeriesCompletionTracker(applicationContext)
    }
    val coroutineScope = rememberCoroutineScope()
    val searchBarScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                when {
                    available.y < -2f -> isSearchBarVisible = false
                    available.y > 2f -> isSearchBarVisible = true
                }
                return Offset.Zero
            }
        }
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

    fun resetSearch() {
        hasSearched = false
        searchResults = emptyList()
        errorMessage = null
        isLoading = false
    }

    fun filtered(
        series: List<SavedSeriesEntity>
    ): List<SavedSeriesEntity> = series.filter { savedSeries ->
        !showFavoritesOnly || savedSeries.isFavorite
    }

    fun markFinishedEpisodes(
        seriesId: Int,
        status: SeriesStatus
    ) {
        if (status == SeriesStatus.FINISHED) {
            coroutineScope.launch {
                completionTracker.markAllEpisodesWatched(seriesId)
            }
        }
    }

    fun searchSeries() {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank() || isLoading) {
            return
        }

        hasSearched = true
        errorMessage = null
        coroutineScope.launch {
            isLoading = true
            try {
                searchResults = TmdbClient.api
                    .searchSeries(cleanQuery)
                    .results
            } catch (exception: Exception) {
                searchResults = emptyList()
                errorMessage = exception.localizedMessage
                    ?: "Series search failed."
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(searchBarScrollConnection)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 66.dp),
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
                    isSearchBarVisible = true
                    resetSearch()
                }
            ) {
                Text(
                    text = if (showFavoritesOnly) "♥" else "♡",
                    color = if (showFavoritesOnly) {
                        LibraryFavoriteColor
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 30.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = isSearchBarVisible) {
            Column {
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
                        label = { Text("Search for a series") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { searchSeries() }
                        )
                    )
                    Button(
                        onClick = { searchSeries() },
                        enabled = query.isNotBlank() && !isLoading
                    ) {
                        Text("Search")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (!hasSearched) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    LibrarySection(
                        title = "En cours",
                        series = filtered(inProgressSeries),
                        onSeriesClick = onSeriesClick,
                        onFavoriteClick = onFavoriteClick,
                        onManageClick = { series -> managedSeries = series }
                    )
                }
                item {
                    LibrarySection(
                        title = "Séries terminées",
                        series = filtered(finishedSeries),
                        onSeriesClick = onSeriesClick,
                        onFavoriteClick = onFavoriteClick,
                        onManageClick = { series -> managedSeries = series }
                    )
                }
                item {
                    LibrarySection(
                        title = "Séries à regarder",
                        series = filtered(toWatchSeries),
                        onSeriesClick = onSeriesClick,
                        onFavoriteClick = onFavoriteClick,
                        onManageClick = { series -> managedSeries = series }
                    )
                }
                item {
                    LibrarySection(
                        title = "Séries arrêtées",
                        series = filtered(stoppedSeries),
                        onSeriesClick = onSeriesClick,
                        onFavoriteClick = onFavoriteClick,
                        onManageClick = { series -> managedSeries = series }
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
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = searchResults,
                                key = { series -> series.id }
                            ) { series ->
                                LibrarySearchResult(
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
        LibraryAddDialog(
            series = series,
            onAdd = { status ->
                onAddSeries(series, status)
                markFinishedEpisodes(
                    seriesId = series.id,
                    status = status
                )
                pendingSeries = null
            },
            onCancel = {
                pendingSeries = null
            }
        )
    }

    managedSeries?.let { series ->
        LibraryManageDialog(
            series = series,
            onStatusChange = { status ->
                onStatusChange(series.id, status)
                markFinishedEpisodes(
                    seriesId = series.id,
                    status = status
                )
                managedSeries = null
            },
            onChangeRating = {
                ratingSeries = series
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

    ratingSeries?.let { series ->
        UserRatingDialog(
            title = "Rate ${series.name}",
            initialRating = series.userRating,
            onSave = { rating ->
                onChangeRating(series.id, rating)
                ratingSeries = null
            },
            onCancel = {
                ratingSeries = null
            }
        )
    }
}

@Composable
private fun LibrarySection(
    title: String,
    series: List<SavedSeriesEntity>,
    onSeriesClick: (Int) -> Unit,
    onFavoriteClick: (
        seriesId: Int,
        isFavorite: Boolean
    ) -> Unit,
    onManageClick: (SavedSeriesEntity) -> Unit
) {
    var showFullList by remember(title) {
        mutableStateOf(false)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showFullList = !showFullList
                }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (showFullList) "⌃" else "⌄",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (series.isEmpty()) {
            Text(
                text = "No series in this category.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        if (showFullList) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                series.chunked(3).forEach { rowSeries ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        rowSeries.forEach { savedSeries ->
                            LibrarySeriesPoster(
                                series = savedSeries,
                                onClick = {
                                    onSeriesClick(savedSeries.id)
                                },
                                onFavoriteClick = {
                                    onFavoriteClick(
                                        savedSeries.id,
                                        !savedSeries.isFavorite
                                    )
                                },
                                onManageClick = {
                                    onManageClick(savedSeries)
                                }
                            )
                        }
                    }
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(
                    items = series,
                    key = { savedSeries -> savedSeries.id }
                ) { savedSeries ->
                    LibrarySeriesPoster(
                        series = savedSeries,
                        onClick = {
                            onSeriesClick(savedSeries.id)
                        },
                        onFavoriteClick = {
                            onFavoriteClick(
                                savedSeries.id,
                                !savedSeries.isFavorite
                            )
                        },
                        onManageClick = {
                            onManageClick(savedSeries)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibrarySeriesPoster(
    series: SavedSeriesEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onManageClick: () -> Unit
) {
    val posterUrl = series.posterPath
        ?.takeIf { path -> path.isNotBlank() }
        ?.let { path -> "$TMDB_LIBRARY_POSTER_BASE_URL$path" }
    var isLoading by remember(posterUrl) {
        mutableStateOf(posterUrl != null)
    }
    var hasError by remember(posterUrl) {
        mutableStateOf(false)
    }

    Column(modifier = Modifier.width(104.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 104.dp, height = 156.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (posterUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
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
                        LibraryFavoriteColor
                    } else {
                        Color.White
                    },
                    fontSize = 20.sp
                )

                UserRatingBadge(
                    rating = series.userRating,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )

                Text(
                    text = "•••",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(onClick = onManageClick)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        MediaCardTitle(title = series.name)
    }
}

@Composable
private fun LibrarySearchResult(
    series: SeriesDto,
    isAdded: Boolean,
    onAddClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            val posterUrl = series.posterPath?.let { path ->
                "$TMDB_LIBRARY_POSTER_BASE_URL$path"
            }

            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 144.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (posterUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${series.name} poster",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = series.name,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
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
                if (series.overview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = series.overview,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAddClick,
                    enabled = !isAdded
                ) {
                    Text(if (isAdded) "Added" else "+ Add")
                }
            }
        }
    }
}

@Composable
private fun LibraryAddDialog(
    series: SeriesDto,
    onAdd: (SeriesStatus) -> Unit,
    onCancel: () -> Unit
) {
    var selectedStatus by remember(series.id) {
        mutableStateOf(SeriesStatus.TO_WATCH)
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add ${series.name}") },
        text = {
            LibraryStatusChoices(
                selectedStatus = selectedStatus,
                onStatusSelected = { status ->
                    selectedStatus = status
                }
            )
        },
        confirmButton = {
            Button(onClick = { onAdd(selectedStatus) }) {
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
private fun LibraryManageDialog(
    series: SavedSeriesEntity,
    onStatusChange: (SeriesStatus) -> Unit,
    onChangeRating: () -> Unit,
    onRemove: () -> Unit,
    onCancel: () -> Unit
) {
    var selectedStatus by remember(series.id, series.status) {
        mutableStateOf(SeriesStatus.fromStorage(series.status))
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(series.name) },
        text = {
            Column {
                Text("Move to")
                Spacer(modifier = Modifier.height(8.dp))
                LibraryStatusChoices(
                    selectedStatus = selectedStatus,
                    onStatusSelected = { status ->
                        selectedStatus = status
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onChangeRating) {
                    Text(
                        if (series.userRating in 1..10) {
                            "Change rating"
                        } else {
                            "Add rating"
                        }
                    )
                }
                TextButton(onClick = onRemove) {
                    Text(
                        text = "Remove series",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onStatusChange(selectedStatus) }) {
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
private fun LibraryStatusChoices(
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
                Text(status.libraryDisplayName())
            }
        }
    }
}

private fun SeriesStatus.libraryDisplayName(): String = when (this) {
    SeriesStatus.IN_PROGRESS -> "En cours"
    SeriesStatus.FINISHED -> "Séries terminées"
    SeriesStatus.TO_WATCH -> "Séries à regarder"
    SeriesStatus.STOPPED -> "Séries arrêtées"
}
