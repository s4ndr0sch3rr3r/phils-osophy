package com.example.phils_osophy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.phils_osophy.data.local.RecipeStatus
import com.example.phils_osophy.data.local.SavedRecipeEntity
import com.example.phils_osophy.ui.components.FavoriteIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RecipeDetailScreen(
    recipe: SavedRecipeEntity,
    onBackClick: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit,
    onStatusChange: (RecipeStatus) -> Unit,
    onSaveDetails: (
        difficulty: String,
        prepTimeMinutes: Int,
        totalTimeMinutes: Int,
        ingredients: String,
        cookingSteps: String
    ) -> Unit,
    onRemoveRecipe: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var difficulty by remember(recipe.key, recipe.difficulty) {
        mutableStateOf(recipe.difficulty)
    }
    var prepTime by remember(recipe.key, recipe.prepTimeMinutes) {
        mutableStateOf(
            recipe.prepTimeMinutes
                .takeIf { minutes -> minutes > 0 }
                ?.toString()
                .orEmpty()
        )
    }
    var totalTime by remember(recipe.key, recipe.totalTimeMinutes) {
        mutableStateOf(
            recipe.totalTimeMinutes
                .takeIf { minutes -> minutes > 0 }
                ?.toString()
                .orEmpty()
        )
    }
    var ingredients by remember(recipe.key, recipe.ingredients) {
        mutableStateOf(recipe.ingredients)
    }
    var cookingSteps by remember(recipe.key, recipe.cookingSteps) {
        mutableStateOf(recipe.cookingSteps)
    }
    val focusManager = LocalFocusManager.current
    val saveButtonBringIntoViewRequester = remember {
        BringIntoViewRequester()
    }
    val coroutineScope = rememberCoroutineScope()
    val imageUrl = remember(recipe.key) {
        "https://picsum.photos/seed/recipe-${recipe.key.hashCode()}/1200/800"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "${recipe.title} image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            TextButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 8.dp, top = 8.dp)
            ) {
                Text("← Back", color = Color.White)
            }

            TextButton(
                onClick = { onFavoriteClick(!recipe.isFavorite) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 116.dp, top = 20.dp)
            ) {
                FavoriteIcon(
                    isFavorite = recipe.isFavorite,
                    size = 30.dp,
                    activeColor = Color(0xFFE53935),
                    inactiveColor = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 66.dp, top = 20.dp)
            ) {
                TextButton(onClick = { isMenuExpanded = true }) {
                    Text(
                        text = "•••",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    RecipeStatus.values().forEach { status ->
                        val isCurrentStatus = status.name == recipe.status
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = buildString {
                                        if (isCurrentStatus) append("✓ ")
                                        append(recipeDetailStatusLabel(status))
                                    }
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                onStatusChange(status)
                            }
                        )
                    }

                    HorizontalDivider()

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Remove recipe",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onRemoveRecipe()
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = recipe.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = recipeDetailStatusLabel(
                        RecipeStatus.fromStorage(recipe.status)
                    ),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = difficulty,
                    onValueChange = { difficulty = it },
                    modifier = Modifier.weight(1.15f),
                    label = { Text("Difficulty") },
                    placeholder = { Text("Easy") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = prepTime,
                    onValueChange = { value ->
                        prepTime = value.filter { character -> character.isDigit() }
                    },
                    modifier = Modifier.weight(0.9f),
                    label = { Text("Prep min") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
                OutlinedTextField(
                    value = totalTime,
                    onValueChange = { value ->
                        totalTime = value.filter { character -> character.isDigit() }
                    },
                    modifier = Modifier.weight(0.9f),
                    label = { Text("Total min") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = ingredients,
                onValueChange = { ingredients = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                delay(300)
                                saveButtonBringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                label = { Text("Ingredients") },
                placeholder = {
                    Text("Add one ingredient per line...")
                },
                minLines = 7,
                maxLines = 14
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = cookingSteps,
                onValueChange = { cookingSteps = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                delay(300)
                                saveButtonBringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                label = { Text("Cooking steps") },
                placeholder = {
                    Text("Describe each cooking step...")
                },
                minLines = 9,
                maxLines = 18
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSaveDetails(
                        difficulty.trim(),
                        prepTime.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                        totalTime.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                        ingredients.trim(),
                        cookingSteps.trim()
                    )
                },
                modifier = Modifier.bringIntoViewRequester(
                    saveButtonBringIntoViewRequester
                )
            ) {
                Text("Save")
            }
        }
    }
}

private fun recipeDetailStatusLabel(status: RecipeStatus): String = when (status) {
    RecipeStatus.IN_PROGRESS -> "En cours"
    RecipeStatus.FINISHED -> "Terminée"
    RecipeStatus.TO_TRY -> "À essayer"
    RecipeStatus.STOPPED -> "Abandonnée"
}
