package com.example.phils_osophy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val InactiveTextColor = Color(0xFF8A8A8A)

private val menuItems = BottomCategory.values().toList()

enum class BottomCategory(val label: String) {
    Series("Series"),
    Movies("Movies"),
    Books("Books"),
    Recipes("Recipes"),
    Games("Games")
}

@Composable
fun ScrollableBottomMenu(
    selectedCategory: BottomCategory?,
    onCategoryClick: (BottomCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val selectedIndex = menuItems.indexOf(selectedCategory)

    LaunchedEffect(selectedIndex) {
        when {
            selectedIndex >= 4 -> {
                listState.animateScrollToItem(
                    (selectedIndex - 3).coerceAtLeast(0)
                )
            }

            selectedIndex in 0..3 -> {
                listState.animateScrollToItem(0)
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier.navigationBarsPadding()
        ) {
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.22f)
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                val itemWidth = maxWidth / 4

                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    state = listState
                ) {
                    items(
                        count = menuItems.size,
                        key = { index -> menuItems[index].name }
                    ) { index ->
                        val item = menuItems[index]
                        val isSelected = item == selectedCategory

                        Column(
                            modifier = Modifier
                                .width(itemWidth)
                                .fillMaxHeight()
                                .clickable {
                                    onCategoryClick(item)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(
                                        if (isSelected) {
                                            Color.White
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(61.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.label,
                                    color = if (isSelected) {
                                        Color.White
                                    } else {
                                        InactiveTextColor
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
