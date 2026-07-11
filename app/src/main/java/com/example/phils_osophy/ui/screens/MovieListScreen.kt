package com.example.phils_osophy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.phils_osophy.data.local.SavedMovieEntity

@Composable
fun MovieListScreen(
    movies: List<SavedMovieEntity>,
    onMovieClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        TextButton(onClick = onBackClick) {
            Text("← Back")
        }

        Text(
            text = "My movie list",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (movies.isEmpty()) {
            Text("No movies added yet.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = movies,
                    key = { movie -> movie.id }
                ) { movie ->
                    Card(
                        onClick = {
                            onMovieClick(movie.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = movie.title,
                                style =
                                    MaterialTheme.typography
                                        .titleMedium
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = movie.releaseDate
                                    .ifBlank {
                                        "Release date unknown"
                                    },
                                style =
                                    MaterialTheme.typography
                                        .bodySmall
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "Rating: %.1f / 10"
                                        .format(
                                            movie.voteAverage
                                        ),
                                style =
                                    MaterialTheme.typography
                                        .bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
