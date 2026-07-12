package com.example.phils_osophy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSeriesDao {

    @Query(
        "SELECT * FROM saved_series " +
            "WHERE status = 'IN_PROGRESS' " +
            "ORDER BY name COLLATE NOCASE ASC"
    )
    fun observeInProgress(): Flow<List<SavedSeriesEntity>>

    @Query(
        "SELECT * FROM saved_series " +
            "WHERE status = 'FINISHED' " +
            "ORDER BY name COLLATE NOCASE ASC"
    )
    fun observeFinished(): Flow<List<SavedSeriesEntity>>

    @Query(
        "SELECT * FROM saved_series " +
            "WHERE status = 'TO_WATCH' " +
            "ORDER BY name COLLATE NOCASE ASC"
    )
    fun observeToWatch(): Flow<List<SavedSeriesEntity>>

    @Query(
        "SELECT * FROM saved_series " +
            "WHERE status = 'STOPPED' " +
            "ORDER BY name COLLATE NOCASE ASC"
    )
    fun observeStopped(): Flow<List<SavedSeriesEntity>>

    @Query("SELECT * FROM saved_series WHERE id = :seriesId LIMIT 1")
    suspend fun getById(seriesId: Int): SavedSeriesEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(series: SavedSeriesEntity)

    @Query(
        "UPDATE saved_series " +
            "SET status = :status " +
            "WHERE id = :seriesId"
    )
    suspend fun updateStatus(
        seriesId: Int,
        status: String
    )

    @Query(
        "UPDATE saved_series " +
            "SET isFavorite = :isFavorite " +
            "WHERE id = :seriesId"
    )
    suspend fun updateFavorite(
        seriesId: Int,
        isFavorite: Boolean
    )

    @Query("DELETE FROM saved_series WHERE id = :seriesId")
    suspend fun deleteById(seriesId: Int)
}
