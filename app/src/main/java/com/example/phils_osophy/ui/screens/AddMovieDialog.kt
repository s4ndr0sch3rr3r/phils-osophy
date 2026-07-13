package com.example.phils_osophy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.phils_osophy.data.remote.MovieDto

private val SelectedStarColor = Color(0xFFFFC107)
private val DialogBackgroundColor = Color(0xFF1E1E1E)

@Composable
fun AddMovieDialog(
    movie: MovieDto,
    onAdd: (rating: Int, note: String) -> Unit,
    onCancel: () -> Unit
) {
    var selectedRating by remember(movie.id) {
        mutableStateOf(0)
    }
    var note by remember(movie.id) {
        mutableStateOf("")
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = DialogBackgroundColor,
            contentColor = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Add ${movie.title}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your rating: $selectedRating / $USER_RATING_MAX",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..USER_RATING_MAX).forEach { star ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clickable {
                                    selectedRating =
                                        if (selectedRating == star) {
                                            0
                                        } else {
                                            star
                                        }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "★",
                                color = if (star <= selectedRating) {
                                    SelectedStarColor
                                } else {
                                    Color.White
                                },
                                fontSize = 22.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Comment (optional)")
                    },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = SelectedStarColor,
                        focusedBorderColor = SelectedStarColor,
                        unfocusedBorderColor = Color.White.copy(
                            alpha = 0.7f
                        ),
                        focusedLabelColor = SelectedStarColor,
                        unfocusedLabelColor = Color.White.copy(
                            alpha = 0.7f
                        )
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onAdd(
                                selectedRating,
                                note.trim()
                            )
                        }
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}
