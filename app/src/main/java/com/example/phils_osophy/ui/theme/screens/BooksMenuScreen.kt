package com.example.phils_osophy.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.phils_osophy.R
import com.example.phils_osophy.ui.components.FullScreenImageMenu
import com.example.phils_osophy.ui.components.InvisibleMenuButton

@Composable
fun BooksMenuScreen(
    onBackClick: () -> Unit,
    onInProgressClick: () -> Unit,
    onFinishedClick: () -> Unit,
    onToReadClick: () -> Unit,
    onAbandonedClick: () -> Unit
) {
    BackHandler {
        onBackClick()
    }

    FullScreenImageMenu(
        imageResId = R.drawable.books_menu,
        contentDescription = "Books menu"
    ) {
        InvisibleMenuButton(
            modifier = Modifier
                .offset(x = maxWidth * 0.05f, y = maxHeight * 0.06f)
                .width(maxWidth * 0.43f)
                .height(maxHeight * 0.42f),
            onClick = onInProgressClick
        )

        InvisibleMenuButton(
            modifier = Modifier
                .offset(x = maxWidth * 0.52f, y = maxHeight * 0.06f)
                .width(maxWidth * 0.43f)
                .height(maxHeight * 0.42f),
            onClick = onFinishedClick
        )

        InvisibleMenuButton(
            modifier = Modifier
                .offset(x = maxWidth * 0.05f, y = maxHeight * 0.52f)
                .width(maxWidth * 0.43f)
                .height(maxHeight * 0.42f),
            onClick = onToReadClick
        )

        InvisibleMenuButton(
            modifier = Modifier
                .offset(x = maxWidth * 0.52f, y = maxHeight * 0.52f)
                .width(maxWidth * 0.43f)
                .height(maxHeight * 0.42f),
            onClick = onAbandonedClick
        )
    }
}