package com.example.phils_osophy.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.phils_osophy.data.remote.SeriesDto

@Entity(tableName = "saved_series")
data class SavedSeriesEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val overview: String,
    val posterPath: String?,
    val firstAirDate: String,
    val voteAverage: Double,
    @ColumnInfo(defaultValue = "0")
    val addedAtEpochMillis: Long,
    val status: String,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean,
    @ColumnInfo(defaultValue = "0")
    val userRating: Int
)

enum class SeriesStatus {
    IN_PROGRESS,
    FINISHED,
    TO_WATCH,
    STOPPED;

    companion object {
        fun fromStorage(value: String): SeriesStatus =
            SeriesStatus.values().firstOrNull { status ->
                status.name == value
            } ?: TO_WATCH
    }
}

fun SeriesDto.toSavedSeriesEntity(
    status: SeriesStatus,
    addedAtEpochMillis: Long = System.currentTimeMillis(),
    isFavorite: Boolean = false,
    userRating: Int = 0
): SavedSeriesEntity =
    SavedSeriesEntity(
        id = id,
        name = name,
        overview = overview,
        posterPath = posterPath,
        firstAirDate = firstAirDate,
        voteAverage = voteAverage,
        addedAtEpochMillis = addedAtEpochMillis,
        status = status.name,
        isFavorite = isFavorite,
        userRating = userRating.coerceIn(0, 10)
    )
