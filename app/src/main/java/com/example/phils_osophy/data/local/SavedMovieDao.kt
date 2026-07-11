package com.example.phils_osophy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMovieDao {

    @Query(
        "SELECT * FROM saved_movies " +
            "ORDER BY title COLLATE NOCASE ASC"
    )
    fun observeAll(): Flow<List<SavedMovieEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(movie: SavedMovieEntity)
}
