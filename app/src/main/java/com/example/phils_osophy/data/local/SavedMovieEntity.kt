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
    val addedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "0")
    val userRating: Int,
    @ColumnInfo(defaultValue = "''")
    val note: String,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean
)

fun MovieDto.toSavedMovieEntity(
    addedAtEpochMillis: Long = System.currentTimeMillis(),
    userRating: Int = 0,
    note: String = "",
    isFavorite: Boolean = false
): SavedMovieEntity =
    SavedMovieEntity(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        addedAtEpochMillis = addedAtEpochMillis,
        userRating = userRating.coerceIn(0, 10),
        note = note,
        isFavorite = isFavorite
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
