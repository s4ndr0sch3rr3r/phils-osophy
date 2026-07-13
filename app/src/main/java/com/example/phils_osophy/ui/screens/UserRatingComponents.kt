package com.example.phils_osophy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val UserRatingBadgeColor = Color(0xFFD32F2F)
private val SelectedRatingStarColor = Color(0xFFFFC107)
private val UnselectedRatingStarColor = Color(0xFF808080)

const val USER_RATING_MAX = 5

@Composable
fun BoxScope.UserRatingBadge(
    rating: Int,
    modifier: Modifier = Modifier
) {
    if (rating !in 1..USER_RATING_MAX) return

    Box(
        modifier = Modifier.align(Alignment.TopEnd),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(UserRatingBadgeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rating.toString(),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InlineUserRatingStars(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedRating = rating.coerceIn(0, USER_RATING_MAX)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..USER_RATING_MAX).forEach { star ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clickable {
                        onRatingChange(
                            if (selectedRating == star) 0 else star
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "★",
                    color = if (star <= selectedRating) {
                        SelectedRatingStarColor
                    } else {
                        UnselectedRatingStarColor
                    },
                    fontSize = 38.sp
                )
            }
        }
    }
}

@Composable
fun UserRatingDialog(
    title: String,
    initialRating: Int,
    onSave: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var selectedRating by remember(title, initialRating) {
        mutableStateOf(initialRating.coerceIn(0, USER_RATING_MAX))
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..USER_RATING_MAX).forEach { star ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable {
                                selectedRating = if (selectedRating == star) 0 else star
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "★",
                            color = if (star <= selectedRating) {
                                SelectedRatingStarColor
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontSize = 21.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedRating) }) {
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
