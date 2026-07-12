package com.example.phils_osophy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppScaffold(
    selectedCategory: BottomCategory?,
    onCategoryClick: (BottomCategory) -> Unit,
    isProfileSelected: Boolean,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val profileEndPadding = when (selectedCategory) {
        BottomCategory.Movies,
        BottomCategory.Series,
        BottomCategory.Books -> 66.dp

        BottomCategory.Games,
        BottomCategory.Explore,
        null -> 16.dp
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            ScrollableBottomMenu(
                selectedCategory = selectedCategory,
                onCategoryClick = onCategoryClick
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            content()

            AlienProfileButton(
                isSelected = isProfileSelected,
                onClick = onProfileClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = profileEndPadding)
            )
        }
    }
}
