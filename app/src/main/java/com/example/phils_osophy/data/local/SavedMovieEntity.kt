package com.example.phils_osophy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.phils_osophy.data.remote.MovieDto

@Entity(tableName = "saved_movies")
data class SavedMovieEntity(
    @PrimaryKey
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val releaseDate: String,
    val voteAverage: Double
)

fun MovieDto.toSavedMovieEntity(): SavedMovieEntity =
    SavedMovieEntity(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage
    )

fun SavedMovieEntity.toMovieDto(): MovieDto =
    MovieDto(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage
    )
