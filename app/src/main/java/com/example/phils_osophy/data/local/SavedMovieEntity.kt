package com.example.phils_osophy.data.local

import androidx.room.ColumnInfo
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
    val voteAverage: Double,
    @ColumnInfo(defaultValue = "0")
    val addedAtEpochMillis: Long
)

fun MovieDto.toSavedMovieEntity(
    addedAtEpochMillis: Long = System.currentTimeMillis()
): SavedMovieEntity =
    SavedMovieEntity(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        addedAtEpochMillis = addedAtEpochMillis
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
