from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GAME_SCREEN = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/GameLibraryScreen.kt"
APP = ROOT / "app/src/main/java/com/example/phils_osophy/App.kt"
ENTITY = ROOT / "app/src/main/java/com/example/phils_osophy/data/local/SavedGameEntity.kt"
DAO = ROOT / "app/src/main/java/com/example/phils_osophy/data/local/SavedGameDao.kt"
DATABASE = ROOT / "app/src/main/java/com/example/phils_osophy/data/local/PhilsOsophyDatabase.kt"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def add_import(text: str, anchor: str, import_line: str) -> str:
    if import_line in text:
        return text
    return replace_once(text, anchor, f"{anchor}{import_line}\n", import_line)


GAME_LIBRARY_BLOCK = r'''@Composable
fun GameLibraryScreen(
    games: List<SavedGameEntity>,
    onAddGame: (
        name: String,
        hoursPlayedMinutes: Int,
        playerCount: Int,
        isFinished: Boolean
    ) -> Unit,
    onChangeRating: (gameId: Long, rating: Int) -> Unit,
    onFavoriteClick: (gameId: Long, isFavorite: Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var searchedQuery by remember { mutableStateOf<String?>(null) }
    var searchResults by remember {
        mutableStateOf<List<GameSearchResult>>(emptyList())
    }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var pendingGameName by remember { mutableStateOf<String?>(null) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var isSearchBarVisible by remember { mutableStateOf(true) }
    var selectedGameId by remember { mutableStateOf<Long?>(null) }

    val apiKey = BuildConfig.RAWG_API_KEY.trim()
    val rawgSummaries = remember {
        mutableStateMapOf<Long, RawgGameSummaryDto?>()
    }
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

    LaunchedEffect(searchedQuery, apiKey) {
        val activeQuery = searchedQuery ?: return@LaunchedEffect
        val localResult = GameSearchResult(
            key = "local:${activeQuery.lowercase(Locale.ROOT)}",
            name = activeQuery,
            imageUrl = null
        )

        isSearching = true
        searchError = null

        if (apiKey.isBlank()) {
            searchResults = listOf(localResult)
            isSearching = false
            return@LaunchedEffect
        }

        try {
            val remoteResults = RawgClient.api.searchGames(
                apiKey = apiKey,
                query = activeQuery,
                pageSize = 12
            ).results
                .filter { result -> result.name.isNotBlank() }
                .distinctBy { result ->
                    result.name.trim().lowercase(Locale.ROOT)
                }
                .map { result ->
                    GameSearchResult(
                        key = "rawg:${result.id}",
                        name = result.name.trim(),
                        imageUrl = result.backgroundImage
                    )
                }

            searchResults = remoteResults.ifEmpty { listOf(localResult) }
        } catch (_: Exception) {
            searchError =
                "Game search could not be loaded. You can still add the entered title."
            searchResults = listOf(localResult)
        } finally {
            isSearching = false
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
                },
                onChangeRating = { rating ->
                    onChangeRating(selectedGame.id, rating)
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

    val visibleGames = remember(games, showFavoritesOnly) {
        games.filter { game ->
            !showFavoritesOnly || game.isFavorite
        }
    }
    val savedGameNames = remember(games) {
        games.map { game ->
            game.name.trim().lowercase(Locale.ROOT)
        }.toSet()
    }

    fun performSearch() {
        searchedQuery = query
            .trim()
            .replace(Regex("\\s+"), " ")
            .takeIf { value -> value.isNotBlank() }
    }

    fun resetSearch() {
        searchedQuery = null
        searchResults = emptyList()
        searchError = null
        isSearching = false
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
                text = "Games (${games.size})",
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
                FavoriteIcon(
                    isFavorite = showFavoritesOnly,
                    size = 30.dp,
                    activeColor = Color(0xFFE53935),
                    inactiveColor = MaterialTheme.colorScheme.onSurface
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
                        label = { Text("Search for a game") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { performSearch() }
                        )
                    )
                    Button(
                        onClick = { performSearch() },
                        enabled = query.isNotBlank() && !isSearching
                    ) {
                        Text("Search")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (searchedQuery == null) {
            when {
                visibleGames.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (showFavoritesOnly) {
                                "No favorite games."
                            } else {
                                "No games added yet."
                            }
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = visibleGames,
                            key = { game -> game.id }
                        ) { game ->
                            GamePosterCard(
                                game = game,
                                imageUrl = rawgSummaries[game.id]
                                    ?.backgroundImage,
                                onClick = {
                                    selectedGameId = game.id
                                },
                                onFavoriteClick = {
                                    onFavoriteClick(
                                        game.id,
                                        !game.isFavorite
                                    )
                                }
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isSearching) {
                    item(key = "game-search-loading") {
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

                searchError?.let { message ->
                    item(key = "game-search-error") {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                searchResults.forEach { result ->
                    item(key = result.key) {
                        GameSearchResultCard(
                            result = result,
                            isAdded = result.name
                                .trim()
                                .lowercase(Locale.ROOT) in savedGameNames,
                            onAddClick = {
                                pendingGameName = result.name
                            }
                        )
                    }
                }
            }
        }
    }

    pendingGameName?.let { initialName ->
        AddGameDialog(
            initialName = initialName,
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
                pendingGameName = null
                query = ""
                resetSearch()
            },
            onCancel = {
                pendingGameName = null
            }
        )
    }
}

private data class GameSearchResult(
    val key: String,
    val name: String,
    val imageUrl: String?
)

@Composable
private fun GameSearchResultCard(
    result: GameSearchResult,
    isAdded: Boolean,
    onAddClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!result.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(result.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${result.name} image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Text(
                text = result.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Button(
                onClick = onAddClick,
                enabled = !isAdded
            ) {
                Text(if (isAdded) "Added" else "Add")
            }
        }
    }
}

@Composable
private fun GamePosterCard(
    game: SavedGameEntity,
    imageUrl: String?,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${game.name} image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                FavoriteIcon(
                    isFavorite = game.isFavorite,
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
                    activeColor = Color(0xFFE53935),
                    inactiveColor = Color.White
                )

                UserRatingBadge(
                    rating = game.userRating,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(26.dp)
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        MediaCardTitle(title = game.name)
    }
}
'''


def update_game_screen() -> None:
    text = GAME_SCREEN.read_text()

    imports = [
        ("import androidx.activity.compose.BackHandler\n", "import androidx.compose.animation.AnimatedVisibility"),
        ("import androidx.compose.foundation.text.KeyboardOptions\n", "import androidx.compose.foundation.text.KeyboardActions"),
        ("import androidx.compose.material3.AlertDialog\n", "import androidx.compose.material3.Button"),
        ("import androidx.compose.ui.Alignment\n", "import androidx.compose.ui.geometry.Offset"),
        ("import androidx.compose.ui.graphics.Color\n", "import androidx.compose.ui.input.nestedscroll.NestedScrollConnection"),
        ("import androidx.compose.ui.input.nestedscroll.NestedScrollConnection\n", "import androidx.compose.ui.input.nestedscroll.NestedScrollSource"),
        ("import androidx.compose.ui.input.nestedscroll.NestedScrollSource\n", "import androidx.compose.ui.input.nestedscroll.nestedScroll"),
        ("import com.example.phils_osophy.data.remote.RawgGameSummaryDto\n", "import com.example.phils_osophy.ui.components.FavoriteIcon"),
    ]
    for anchor, import_line in imports:
        text = add_import(text, anchor, import_line)

    start_marker = "@Composable\nfun GameLibraryScreen("
    end_marker = "@Composable\nprivate fun GameDetailContent("
    start = text.find(start_marker)
    end = text.find(end_marker)
    if start == -1 or end == -1 or end <= start:
        raise RuntimeError("Could not locate Game library block")
    text = text[:start] + GAME_LIBRARY_BLOCK + "\n" + text[end:]

    text = replace_once(
        text,
        "private fun AddGameDialog(\n    onAdd: (",
        "private fun AddGameDialog(\n    initialName: String = \"\",\n    onAdd: (",
        "AddGameDialog initialName parameter"
    )
    text = replace_once(
        text,
        "    var name by remember {\n        mutableStateOf(\"\")\n    }",
        "    var name by remember(initialName) {\n        mutableStateOf(initialName)\n    }",
        "AddGameDialog initial name state"
    )

    GAME_SCREEN.write_text(text)


def update_app() -> None:
    text = APP.read_text()
    old = '''                    onChangeRating = { gameId, rating ->
                        coroutineScope.launch {
                            savedGameDao.updateRating(
                                gameId = gameId,
                                userRating = rating.coerceIn(0, USER_RATING_MAX)
                            )
                        }
                    },
                    onBackClick = ::openMovies'''
    new = '''                    onChangeRating = { gameId, rating ->
                        coroutineScope.launch {
                            savedGameDao.updateRating(
                                gameId = gameId,
                                userRating = rating.coerceIn(0, USER_RATING_MAX)
                            )
                        }
                    },
                    onFavoriteClick = { gameId, isFavorite ->
                        coroutineScope.launch {
                            savedGameDao.updateFavorite(
                                gameId = gameId,
                                isFavorite = isFavorite
                            )
                        }
                    },
                    onBackClick = ::openMovies'''
    text = replace_once(text, old, new, "Game favorite callback")
    APP.write_text(text)


def update_entity() -> None:
    text = ENTITY.read_text()
    old = '''    @ColumnInfo(defaultValue = "0")
    val userRating: Int = 0
)'''
    new = '''    @ColumnInfo(defaultValue = "0")
    val userRating: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false
)'''
    text = replace_once(text, old, new, "SavedGameEntity favorite")
    ENTITY.write_text(text)


def update_dao() -> None:
    text = DAO.read_text()
    old = '''    @Query(
        "UPDATE saved_games SET userRating = :userRating WHERE id = :gameId"
    )
    suspend fun updateRating(gameId: Long, userRating: Int)
}'''
    new = '''    @Query(
        "UPDATE saved_games SET userRating = :userRating WHERE id = :gameId"
    )
    suspend fun updateRating(gameId: Long, userRating: Int)

    @Query(
        "UPDATE saved_games SET isFavorite = :isFavorite WHERE id = :gameId"
    )
    suspend fun updateFavorite(gameId: Long, isFavorite: Boolean)
}'''
    text = replace_once(text, old, new, "SavedGameDao favorite")
    DAO.write_text(text)


def update_database() -> None:
    text = DATABASE.read_text()
    text = replace_once(text, "    version = 15,", "    version = 16,", "database version")

    migration = '''
        private val migration15To16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE saved_games ADD COLUMN isFavorite " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
'''
    anchor = "\n        fun getInstance(context: Context): PhilsOsophyDatabase ="
    if migration.strip() not in text:
        text = replace_once(text, anchor, migration + anchor, "migration 15 to 16")

    text = replace_once(
        text,
        "                        migration14To15\n",
        "                        migration14To15,\n                        migration15To16\n",
        "migration registration"
    )
    DATABASE.write_text(text)


update_game_screen()
update_app()
update_entity()
update_dao()
update_database()
