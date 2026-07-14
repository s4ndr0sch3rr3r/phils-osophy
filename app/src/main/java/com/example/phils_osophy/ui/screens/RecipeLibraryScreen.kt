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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.phils_osophy.data.local.RecipeStatus
import com.example.phils_osophy.data.local.SavedRecipeEntity
import com.example.phils_osophy.ui.components.FavoriteIcon
import java.util.Locale

private val RecipeFavoriteColor = Color(0xFFE53935)

@Composable
fun RecipeLibraryScreen(
    inProgressRecipes: List<SavedRecipeEntity>,
    finishedRecipes: List<SavedRecipeEntity>,
    toTryRecipes: List<SavedRecipeEntity>,
    stoppedRecipes: List<SavedRecipeEntity>,
    onAddRecipe: (title: String, status: RecipeStatus) -> Unit,
    onRecipeClick: (recipeKey: String) -> Unit,
    onFavoriteClick: (recipeKey: String, isFavorite: Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    var query by remember { mutableStateOf("") }
    var searchedTitle by remember { mutableStateOf<String?>(null) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var isSearchBarVisible by remember { mutableStateOf(true) }
    var pendingRecipeTitle by remember { mutableStateOf<String?>(null) }
    val expandedSections = remember {
        mutableStateMapOf<String, Boolean>()
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

    val allRecipes = remember(
        inProgressRecipes,
        finishedRecipes,
        toTryRecipes,
        stoppedRecipes
    ) {
        (
            inProgressRecipes +
                finishedRecipes +
                toTryRecipes +
                stoppedRecipes
            ).distinctBy { recipe -> recipe.key }
    }
    val savedRecipeKeys = remember(allRecipes) {
        allRecipes.map { recipe -> recipe.key }.toSet()
    }

    fun filtered(recipes: List<SavedRecipeEntity>): List<SavedRecipeEntity> =
        recipes.filter { recipe ->
            !showFavoritesOnly || recipe.isFavorite
        }

    fun performSearch() {
        searchedTitle = query.trim()
            .replace(Regex("\\s+"), " ")
            .takeIf { title -> title.isNotBlank() }
    }

    fun resetSearch() {
        searchedTitle = null
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
                text = "Recipes",
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
                    activeColor = RecipeFavoriteColor,
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
                        label = { Text("Search for a recipe") },
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
                        enabled = query.isNotBlank()
                    ) {
                        Text("Search")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (searchedTitle == null) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                recipeSectionItems(
                    title = "En cours",
                    recipes = filtered(inProgressRecipes),
                    isExpanded = expandedSections["En cours"] == true,
                    onToggleExpanded = {
                        expandedSections["En cours"] =
                            expandedSections["En cours"] != true
                    },
                    onRecipeClick = onRecipeClick,
                    onFavoriteClick = onFavoriteClick
                )
                recipeSectionItems(
                    title = "Recettes terminées",
                    recipes = filtered(finishedRecipes),
                    isExpanded = expandedSections["Recettes terminées"] == true,
                    onToggleExpanded = {
                        expandedSections["Recettes terminées"] =
                            expandedSections["Recettes terminées"] != true
                    },
                    onRecipeClick = onRecipeClick,
                    onFavoriteClick = onFavoriteClick
                )
                recipeSectionItems(
                    title = "Recettes à essayer",
                    recipes = filtered(toTryRecipes),
                    isExpanded = expandedSections["Recettes à essayer"] == true,
                    onToggleExpanded = {
                        expandedSections["Recettes à essayer"] =
                            expandedSections["Recettes à essayer"] != true
                    },
                    onRecipeClick = onRecipeClick,
                    onFavoriteClick = onFavoriteClick
                )
                recipeSectionItems(
                    title = "Recettes abandonnées",
                    recipes = filtered(stoppedRecipes),
                    isExpanded = expandedSections["Recettes abandonnées"] == true,
                    onToggleExpanded = {
                        expandedSections["Recettes abandonnées"] =
                            expandedSections["Recettes abandonnées"] != true
                    },
                    onRecipeClick = onRecipeClick,
                    onFavoriteClick = onFavoriteClick
                )
            }
        } else {
            val title = searchedTitle.orEmpty()
            val recipeKey = title.lowercase(Locale.ROOT)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                RecipeSearchResult(
                    title = title,
                    isAdded = recipeKey in savedRecipeKeys,
                    modifier = Modifier.fillMaxWidth(),
                    onAddClick = {
                        pendingRecipeTitle = title
                    }
                )
            }
        }
    }

    pendingRecipeTitle?.let { title ->
        RecipeAddDialog(
            title = title,
            onAdd = { status ->
                onAddRecipe(title, status)
                pendingRecipeTitle = null
                query = ""
                resetSearch()
            },
            onCancel = {
                pendingRecipeTitle = null
            }
        )
    }

}

private fun LazyListScope.recipeSectionItems(
    title: String,
    recipes: List<SavedRecipeEntity>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onRecipeClick: (String) -> Unit,
    onFavoriteClick: (recipeKey: String, isFavorite: Boolean) -> Unit
) {
    val sectionKey = "recipe-section-$title"

    item(
        key = "$sectionKey-header",
        contentType = "library-section-header"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
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
                text = if (isExpanded) "⌃" else "⌄",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    item(
        key = "$sectionKey-top-gap",
        contentType = "library-section-gap"
    ) {
        Spacer(modifier = Modifier.height(10.dp))
    }

    when {
        recipes.isEmpty() -> {
            item(
                key = "$sectionKey-empty",
                contentType = "library-section-empty"
            ) {
                Text(
                    text = "No recipes in this category.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        isExpanded -> {
            val rowCount = (recipes.size + 2) / 3
            items(
                count = rowCount,
                key = { rowIndex ->
                    "$sectionKey-row-${recipes[rowIndex * 3].key}"
                },
                contentType = { "recipe-grid-row" }
            ) { rowIndex ->
                val startIndex = rowIndex * 3
                val endIndex = minOf(startIndex + 3, recipes.size)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    for (recipeIndex in startIndex until endIndex) {
                        val recipe = recipes[recipeIndex]
                        RecipeCard(
                            recipe = recipe,
                            onClick = {
                                onRecipeClick(recipe.key)
                            },
                            onFavoriteClick = {
                                onFavoriteClick(recipe.key, !recipe.isFavorite)
                            }
                        )
                    }
                }
            }
        }

        else -> {
            item(
                key = "$sectionKey-preview",
                contentType = "recipe-horizontal-preview"
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    items(
                        items = recipes,
                        key = { recipe -> recipe.key },
                        contentType = { "recipe-card" }
                    ) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = {
                                onRecipeClick(recipe.key)
                            },
                            onFavoriteClick = {
                                onFavoriteClick(recipe.key, !recipe.isFavorite)
                            }
                        )
                    }
                }
            }
        }
    }

    item(
        key = "$sectionKey-bottom-gap",
        contentType = "library-section-gap"
    ) {
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun RecipeCard(
    recipe: SavedRecipeEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Column(modifier = Modifier.width(104.dp)) {
        Card(
            modifier = Modifier
                .size(104.dp)
                .clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                FavoriteIcon(
                    isFavorite = recipe.isFavorite,
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
                    activeColor = RecipeFavoriteColor,
                    inactiveColor = Color.White
                )
            }
        }
        MediaCardTitle(title = recipe.title)
    }
}

@Composable
private fun RecipeSearchResult(
    title: String,
    isAdded: Boolean,
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Text(
                text = title,
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
private fun RecipeAddDialog(
    title: String,
    onAdd: (RecipeStatus) -> Unit,
    onCancel: () -> Unit
) {
    var selectedStatus by remember(title) {
        mutableStateOf(RecipeStatus.TO_TRY)
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add recipe") },
        text = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                RecipeStatus.values().forEach { status ->
                    RecipeStatusRow(
                        status = status,
                        selected = status == selectedStatus,
                        onClick = {
                            selectedStatus = status
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(selectedStatus) }) {
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
private fun RecipeStatusRow(
    status: RecipeStatus,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(recipeStatusLabel(status))
    }
}

private fun recipeStatusLabel(status: RecipeStatus): String = when (status) {
    RecipeStatus.IN_PROGRESS -> "En cours"
    RecipeStatus.FINISHED -> "Terminée"
    RecipeStatus.TO_TRY -> "À essayer"
    RecipeStatus.STOPPED -> "Abandonnée"
}
