package com.example.phils_osophy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedEpisodeDao {

    @Query(
        "SELECT * FROM watched_episodes " +
            "WHERE seriesId = :seriesId " +
            "ORDER BY seasonNumber, episodeNumber"
    )
    fun observeForSeries(seriesId: Int): Flow<List<WatchedEpisodeEntity>>

    @Query(
        "SELECT * FROM watched_episodes " +
            "WHERE seriesId = :seriesId " +
            "ORDER BY seasonNumber, episodeNumber"
    )
    suspend fun getForSeries(seriesId: Int): List<WatchedEpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markWatched(episode: WatchedEpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markWatched(episodes: List<WatchedEpisodeEntity>)

    @Query(
        "DELETE FROM watched_episodes " +
            "WHERE seriesId = :seriesId " +
            "AND seasonNumber = :seasonNumber " +
            "AND episodeNumber = :episodeNumber"
    )
    suspend fun markUnwatched(
        seriesId: Int,
        seasonNumber: Int,
        episodeNumber: Int
    )

    @Query(
        "DELETE FROM watched_episodes " +
            "WHERE seriesId = :seriesId " +
            "AND seasonNumber = :seasonNumber"
    )
    suspend fun markSeasonUnwatched(
        seriesId: Int,
        seasonNumber: Int
    )

    @Query("DELETE FROM watched_episodes WHERE seriesId = :seriesId")
    suspend fun deleteForSeries(seriesId: Int)
}
