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

    @Query(
        "UPDATE saved_movies " +
            "SET isFavorite = :isFavorite " +
            "WHERE id = :movieId"
    )
    suspend fun updateFavorite(
        movieId: Int,
        isFavorite: Boolean
    )

    @Query(
        "UPDATE saved_movies " +
            "SET userRating = :userRating " +
            "WHERE id = :movieId"
    )
    suspend fun updateRating(
        movieId: Int,
        userRating: Int
    )

    @Query(
        "UPDATE saved_movies " +
            "SET note = :note " +
            "WHERE id = :movieId"
    )
    suspend fun updateNote(
        movieId: Int,
        note: String
    )

    @Query("DELETE FROM saved_movies WHERE id = :movieId")
    suspend fun deleteById(movieId: Int)
}
