from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/example/phils_osophy/App.kt"
DATABASE = ROOT / "app/src/main/java/com/example/phils_osophy/data/local/PhilsOsophyDatabase.kt"
LIBRARY = ROOT / "app/src/main/java/com/example/phils_osophy/ui/screens/RecipeLibraryScreen.kt"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def update_app() -> None:
    text = APP.read_text()

    text = replace_once(
        text,
        "import com.example.phils_osophy.ui.screens.RecipeLibraryScreen\n",
        "import com.example.phils_osophy.ui.screens.RecipeDetailScreen\n"
        "import com.example.phils_osophy.ui.screens.RecipeLibraryScreen\n",
        "Recipe detail import",
    )

    text = replace_once(
        text,
        "    var pendingMovie by remember {\n"
        "        mutableStateOf<MovieDto?>(null)\n"
        "    }\n",
        "    var pendingMovie by remember {\n"
        "        mutableStateOf<MovieDto?>(null)\n"
        "    }\n"
        "    var selectedRecipeKey by remember {\n"
        "        mutableStateOf<String?>(null)\n"
        "    }\n",
        "selected Recipe state",
    )

    text = replace_once(
        text,
        "    val stoppedRecipes by stoppedRecipesFlow.collectAsState(\n"
        "        initial = emptyList()\n"
        "    )\n\n"
        "    val inProgressSeriesFlow = remember(savedSeriesDao) {\n",
        "    val stoppedRecipes by stoppedRecipesFlow.collectAsState(\n"
        "        initial = emptyList()\n"
        "    )\n\n"
        "    val allSavedRecipes = remember(\n"
        "        inProgressRecipes,\n"
        "        finishedRecipes,\n"
        "        toTryRecipes,\n"
        "        stoppedRecipes\n"
        "    ) {\n"
        "        (\n"
        "            inProgressRecipes +\n"
        "                finishedRecipes +\n"
        "                toTryRecipes +\n"
        "                stoppedRecipes\n"
        "            ).distinctBy { recipe -> recipe.key }\n"
        "    }\n\n"
        "    val inProgressSeriesFlow = remember(savedSeriesDao) {\n",
        "combined Recipe list",
    )

    text = replace_once(
        text,
        "    fun clearSelectedSeries() {\n"
        "        selectedSeriesId = null\n"
        "        selectedSeasonNumber = null\n"
        "        selectedEpisodeNumber = null\n"
        "    }\n\n"
        "    fun openMovies() {\n",
        "    fun clearSelectedSeries() {\n"
        "        selectedSeriesId = null\n"
        "        selectedSeasonNumber = null\n"
        "        selectedEpisodeNumber = null\n"
        "    }\n\n"
        "    fun clearSelectedRecipe() {\n"
        "        selectedRecipeKey = null\n"
        "    }\n\n"
        "    fun openRecipes() {\n"
        "        clearSelectedRecipe()\n"
        "        currentScreen = AppScreen.RecipesMenu\n"
        "    }\n\n"
        "    fun openMovies() {\n",
        "Recipe navigation helpers",
    )

    text = replace_once(
        text,
        "        clearSelectedSeries()\n"
        "        currentScreen = AppScreen.MoviesMenu\n",
        "        clearSelectedSeries()\n"
        "        clearSelectedRecipe()\n"
        "        currentScreen = AppScreen.MoviesMenu\n",
        "clear Recipe when opening Movies",
    )

    text = replace_once(
        text,
        "        clearSelectedSeries()\n"
        "        currentScreen = AppScreen.Profile\n",
        "        clearSelectedSeries()\n"
        "        clearSelectedRecipe()\n"
        "        currentScreen = AppScreen.Profile\n",
        "clear Recipe when opening Profile",
    )

    text = replace_once(
        text,
        "            AppScreen.GamesMenu,\n"
        "            AppScreen.BooksMenu,\n"
        "            AppScreen.RecipesMenu -> openMovies()\n\n"
        "            AppScreen.MoviesMenu -> Unit\n",
        "            AppScreen.GamesMenu,\n"
        "            AppScreen.BooksMenu,\n"
        "            AppScreen.RecipesMenu -> openMovies()\n\n"
        "            AppScreen.RecipeDetail -> openRecipes()\n"
        "            AppScreen.MoviesMenu -> Unit\n",
        "Recipe back behavior",
    )

    text = replace_once(
        text,
        "            clearSelectedSeries()\n"
        "            currentScreen = category.toAppScreen()\n",
        "            clearSelectedSeries()\n"
        "            clearSelectedRecipe()\n"
        "            currentScreen = category.toAppScreen()\n",
        "clear Recipe on bottom navigation",
    )

    old_recipe_case = """            AppScreen.RecipesMenu -> {
                RecipeLibraryScreen(
                    inProgressRecipes = inProgressRecipes,
                    finishedRecipes = finishedRecipes,
                    toTryRecipes = toTryRecipes,
                    stoppedRecipes = stoppedRecipes,
                    onAddRecipe = { title, status ->
                        coroutineScope.launch {
                            savedRecipeDao.insert(
                                createSavedRecipeEntity(
                                    title = title,
                                    status = status
                                )
                            )
                        }
                    },
                    onFavoriteClick = { recipeKey, isFavorite ->
                        coroutineScope.launch {
                            savedRecipeDao.updateFavorite(
                                recipeKey = recipeKey,
                                isFavorite = isFavorite
                            )
                        }
                    },
                    onStatusChange = { recipeKey, status ->
                        coroutineScope.launch {
                            savedRecipeDao.updateStatus(
                                recipeKey = recipeKey,
                                status = status.name
                            )
                        }
                    },
                    onRemoveRecipe = { recipeKey ->
                        coroutineScope.launch {
                            savedRecipeDao.deleteByKey(recipeKey)
                        }
                    },
                    onBackClick = ::openMovies
                )
            }
"""
    new_recipe_cases = """            AppScreen.RecipesMenu -> {
                RecipeLibraryScreen(
                    inProgressRecipes = inProgressRecipes,
                    finishedRecipes = finishedRecipes,
                    toTryRecipes = toTryRecipes,
                    stoppedRecipes = stoppedRecipes,
                    onAddRecipe = { title, status ->
                        coroutineScope.launch {
                            savedRecipeDao.insert(
                                createSavedRecipeEntity(
                                    title = title,
                                    status = status
                                )
                            )
                        }
                    },
                    onRecipeClick = { recipeKey ->
                        selectedRecipeKey = recipeKey
                        currentScreen = AppScreen.RecipeDetail
                    },
                    onFavoriteClick = { recipeKey, isFavorite ->
                        coroutineScope.launch {
                            savedRecipeDao.updateFavorite(
                                recipeKey = recipeKey,
                                isFavorite = isFavorite
                            )
                        }
                    },
                    onBackClick = ::openMovies
                )
            }

            AppScreen.RecipeDetail -> {
                val selectedRecipe = allSavedRecipes.firstOrNull { recipe ->
                    recipe.key == selectedRecipeKey
                }

                if (selectedRecipe != null) {
                    RecipeDetailScreen(
                        recipe = selectedRecipe,
                        onBackClick = ::openRecipes,
                        onFavoriteClick = { isFavorite ->
                            coroutineScope.launch {
                                savedRecipeDao.updateFavorite(
                                    recipeKey = selectedRecipe.key,
                                    isFavorite = isFavorite
                                )
                            }
                        },
                        onStatusChange = { status ->
                            coroutineScope.launch {
                                savedRecipeDao.updateStatus(
                                    recipeKey = selectedRecipe.key,
                                    status = status.name
                                )
                            }
                        },
                        onSaveDetails = {
                                difficulty,
                                prepTimeMinutes,
                                totalTimeMinutes,
                                ingredients,
                                cookingSteps ->
                            coroutineScope.launch {
                                savedRecipeDao.updateDetails(
                                    recipeKey = selectedRecipe.key,
                                    difficulty = difficulty,
                                    prepTimeMinutes = prepTimeMinutes,
                                    totalTimeMinutes = totalTimeMinutes,
                                    ingredients = ingredients,
                                    cookingSteps = cookingSteps
                                )
                            }
                        },
                        onRemoveRecipe = {
                            coroutineScope.launch {
                                savedRecipeDao.deleteByKey(selectedRecipe.key)
                                openRecipes()
                            }
                        }
                    )
                } else {
                    EmptyPageScreen(
                        title = "Recipe unavailable",
                        onBackClick = ::openRecipes
                    )
                }
            }
"""
    text = replace_once(
        text,
        old_recipe_case,
        new_recipe_cases,
        "Recipe screen cases",
    )

    text = replace_once(
        text,
        "    AppScreen.BooksMenu -> BottomCategory.Books\n"
        "    AppScreen.RecipesMenu -> BottomCategory.Recipes\n",
        "    AppScreen.BooksMenu -> BottomCategory.Books\n"
        "    AppScreen.RecipesMenu,\n"
        "    AppScreen.RecipeDetail -> BottomCategory.Recipes\n",
        "Recipe bottom category",
    )

    APP.write_text(text)


def update_database() -> None:
    text = DATABASE.read_text()

    if "version = 15" not in text:
        text = replace_once(
            text,
            "    version = 14,\n",
            "    version = 15,\n",
            "database version",
        )

    migration = """        private val migration14To15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE saved_recipes ADD COLUMN difficulty " +
                        "TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE saved_recipes ADD COLUMN prepTimeMinutes " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE saved_recipes ADD COLUMN totalTimeMinutes " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE saved_recipes ADD COLUMN ingredients " +
                        "TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE saved_recipes ADD COLUMN cookingSteps " +
                        "TEXT NOT NULL DEFAULT ''"
                )
            }
        }

"""
    if "migration14To15" not in text:
        marker = "        fun getInstance(context: Context): PhilsOsophyDatabase =\n"
        if marker not in text:
            raise RuntimeError("database migration insertion marker not found")
        text = text.replace(marker, migration + marker, 1)

    if "migration14To15\n" not in text.split(".addMigrations(", 1)[1]:
        text = replace_once(
            text,
            "                        migration12To13,\n"
            "                        migration13To14\n",
            "                        migration12To13,\n"
            "                        migration13To14,\n"
            "                        migration14To15\n",
            "database migration registration",
        )

    DATABASE.write_text(text)


def update_library() -> None:
    text = LIBRARY.read_text()

    text = replace_once(
        text,
        "    onAddRecipe: (title: String, status: RecipeStatus) -> Unit,\n"
        "    onFavoriteClick: (recipeKey: String, isFavorite: Boolean) -> Unit,\n"
        "    onStatusChange: (recipeKey: String, status: RecipeStatus) -> Unit,\n"
        "    onRemoveRecipe: (recipeKey: String) -> Unit,\n"
        "    onBackClick: () -> Unit\n",
        "    onAddRecipe: (title: String, status: RecipeStatus) -> Unit,\n"
        "    onRecipeClick: (recipeKey: String) -> Unit,\n"
        "    onFavoriteClick: (recipeKey: String, isFavorite: Boolean) -> Unit,\n"
        "    onBackClick: () -> Unit\n",
        "Recipe library callbacks",
    )

    text = text.replace(
        "    var selectedRecipeKey by remember { mutableStateOf<String?>(null) }\n",
        "",
    )

    text = re.sub(
        r"    val selectedRecipe = allRecipes\.firstOrNull \{ recipe ->\n"
        r"        recipe\.key == selectedRecipeKey\n"
        r"    \}\n",
        "",
        text,
        count=1,
    )

    callback_block = (
        "                    onRecipeClick = { recipeKey ->\n"
        "                        selectedRecipeKey = recipeKey\n"
        "                    },\n"
    )
    if callback_block in text:
        text = text.replace(
            callback_block,
            "                    onRecipeClick = onRecipeClick,\n",
        )

    text = re.sub(
        r"\n    selectedRecipe\?\.let \{ recipe ->\n"
        r"        RecipeManageDialog\([\s\S]*?\n"
        r"        \)\n"
        r"    \}\n",
        "\n",
        text,
        count=1,
    )

    text = re.sub(
        r"\n@Composable\nprivate fun RecipeManageDialog\([\s\S]*?\n"
        r"\}\n\n(?=@Composable\nprivate fun RecipeStatusRow)",
        "\n",
        text,
        count=1,
    )

    if "selectedRecipeKey" in text or "RecipeManageDialog" in text:
        raise RuntimeError("legacy Recipe card-management state remains")

    LIBRARY.write_text(text)


update_app()
update_database()
update_library()
