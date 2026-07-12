package com.example.phils_osophy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private val AlienGreen = Color(0xFF7CFF6B)
private val AlienDarkGreen = Color(0xFF123C1A)
private val AlienSelectedBorder = Color(0xFFB7FF9E)

@Composable
fun AlienProfileButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(42.dp),
        shape = CircleShape,
        color = AlienDarkGreen,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                AlienSelectedBorder
            } else {
                AlienGreen.copy(alpha = 0.65f)
            }
        )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            val strokeWidth = size.minDimension * 0.06f
            val centerX = size.width / 2f

            drawLine(
                color = AlienGreen,
                start = Offset(size.width * 0.36f, size.height * 0.26f),
                end = Offset(size.width * 0.22f, size.height * 0.08f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = AlienGreen,
                start = Offset(size.width * 0.64f, size.height * 0.26f),
                end = Offset(size.width * 0.78f, size.height * 0.08f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = AlienGreen,
                radius = size.minDimension * 0.055f,
                center = Offset(size.width * 0.20f, size.height * 0.06f)
            )
            drawCircle(
                color = AlienGreen,
                radius = size.minDimension * 0.055f,
                center = Offset(size.width * 0.80f, size.height * 0.06f)
            )

            drawOval(
                color = AlienGreen,
                topLeft = Offset(size.width * 0.13f, size.height * 0.20f),
                size = Size(size.width * 0.74f, size.height * 0.66f)
            )

            drawOval(
                color = Color.Black,
                topLeft = Offset(size.width * 0.27f, size.height * 0.40f),
                size = Size(size.width * 0.15f, size.height * 0.20f)
            )
            drawOval(
                color = Color.Black,
                topLeft = Offset(size.width * 0.58f, size.height * 0.40f),
                size = Size(size.width * 0.15f, size.height * 0.20f)
            )

            drawArc(
                color = Color.Black,
                startAngle = 18f,
                sweepAngle = 144f,
                useCenter = false,
                topLeft = Offset(centerX - size.width * 0.16f, size.height * 0.58f),
                size = Size(size.width * 0.32f, size.height * 0.16f),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
