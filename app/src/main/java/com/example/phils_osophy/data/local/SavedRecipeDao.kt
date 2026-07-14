package com.example.phils_osophy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRecipeDao {

    @Query(
        "SELECT * FROM saved_recipes " +
            "WHERE status = 'IN_PROGRESS' " +
            "ORDER BY title COLLATE NOCASE ASC"
    )
    fun observeInProgress(): Flow<List<SavedRecipeEntity>>

    @Query(
        "SELECT * FROM saved_recipes " +
            "WHERE status = 'FINISHED' " +
            "ORDER BY title COLLATE NOCASE ASC"
    )
    fun observeFinished(): Flow<List<SavedRecipeEntity>>

    @Query(
        "SELECT * FROM saved_recipes " +
            "WHERE status = 'TO_TRY' " +
            "ORDER BY title COLLATE NOCASE ASC"
    )
    fun observeToTry(): Flow<List<SavedRecipeEntity>>

    @Query(
        "SELECT * FROM saved_recipes " +
            "WHERE status = 'STOPPED' " +
            "ORDER BY title COLLATE NOCASE ASC"
    )
    fun observeStopped(): Flow<List<SavedRecipeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(recipe: SavedRecipeEntity)

    @Query(
        "UPDATE saved_recipes SET status = :status WHERE `key` = :recipeKey"
    )
    suspend fun updateStatus(recipeKey: String, status: String)

    @Query(
        "UPDATE saved_recipes " +
            "SET isFavorite = :isFavorite " +
            "WHERE `key` = :recipeKey"
    )
    suspend fun updateFavorite(recipeKey: String, isFavorite: Boolean)

    @Query("DELETE FROM saved_recipes WHERE `key` = :recipeKey")
    suspend fun deleteByKey(recipeKey: String)
}
