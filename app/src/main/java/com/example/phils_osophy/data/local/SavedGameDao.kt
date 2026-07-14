package com.example.phils_osophy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGameDao {

    @Query(
        "SELECT * FROM saved_games " +
            "ORDER BY addedAtEpochMillis DESC, id DESC"
    )
    fun observeAll(): Flow<List<SavedGameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: SavedGameEntity)

    @Query(
        "UPDATE saved_games SET userRating = :userRating WHERE id = :gameId"
    )
    suspend fun updateRating(gameId: Long, userRating: Int)

    @Query(
        "UPDATE saved_games SET isFavorite = :isFavorite WHERE id = :gameId"
    )
    suspend fun updateFavorite(gameId: Long, isFavorite: Boolean)
}
