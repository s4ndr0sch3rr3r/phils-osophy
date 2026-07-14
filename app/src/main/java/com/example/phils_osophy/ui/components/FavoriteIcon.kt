package com.example.phils_osophy.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val FavoriteRed = Color(0xFFE53935)

@Composable
fun FavoriteIcon(
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    activeColor: Color = FavoriteRed,
    inactiveColor: Color = Color.White
) {
    Icon(
        imageVector = if (isFavorite) {
            FilledFavoriteIcon
        } else {
            OutlinedFavoriteIcon
        },
        contentDescription = if (isFavorite) {
            "Remove from favorites"
        } else {
            "Add to favorites"
        },
        modifier = modifier.size(size),
        tint = if (isFavorite) activeColor else inactiveColor
    )
}

private val FilledFavoriteIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "FilledFavorite",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 21.35f)
            lineTo(10.55f, 20.03f)
            curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
            curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
            curveTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
            curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
            curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
            curveTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.04f)
            lineTo(12f, 21.35f)
            close()
        }
    }.build()
}

private val OutlinedFavoriteIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "OutlinedFavorite",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(16.5f, 3f)
            curveTo(14.76f, 3f, 13.09f, 3.81f, 12f, 5.09f)
            curveTo(10.91f, 3.81f, 9.24f, 3f, 7.5f, 3f)
            curveTo(4.42f, 3f, 2f, 5.42f, 2f, 8.5f)
            curveTo(2f, 12.28f, 5.4f, 15.36f, 10.55f, 20.04f)
            lineTo(12f, 21.35f)
            lineTo(13.45f, 20.03f)
            curveTo(18.6f, 15.36f, 22f, 12.28f, 22f, 8.5f)
            curveTo(22f, 5.42f, 19.58f, 3f, 16.5f, 3f)
            close()

            moveTo(12.1f, 18.55f)
            lineTo(12f, 18.65f)
            lineTo(11.9f, 18.55f)
            curveTo(7.14f, 14.24f, 4f, 11.39f, 4f, 8.5f)
            curveTo(4f, 6.5f, 5.5f, 5f, 7.5f, 5f)
            curveTo(9.04f, 5f, 10.54f, 5.99f, 11.07f, 7.36f)
            lineTo(12.93f, 7.36f)
            curveTo(13.46f, 5.99f, 14.96f, 5f, 16.5f, 5f)
            curveTo(18.5f, 5f, 20f, 6.5f, 20f, 8.5f)
            curveTo(20f, 11.39f, 16.86f, 14.24f, 12.1f, 18.55f)
            close()
        }
    }.build()
}