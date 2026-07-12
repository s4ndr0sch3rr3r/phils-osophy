package com.example.phils_osophy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedBookDao {

    @Query(
        "SELECT * FROM saved_books " +
            "ORDER BY title COLLATE NOCASE ASC"
    )
    fun observeAll(): Flow<List<SavedBookEntity>>

    @Query(
        "SELECT * FROM saved_books " +
            "WHERE status = 'IN_PROGRESS' " +
            "ORDER BY title COLLATE NOCASE ASC"
    )
    fun observeInProgress(): Flow<List<SavedBookEntity>>

    @Query(
        "SELECT * FROM saved_books " +
            "WHERE status = 'FINISHED' " +
            "ORDER BY title COLLATE NOCASE ASC"
    )
    fun observeFinished(): Flow<List<SavedBookEntity>>

    @Query(
        "SELECT * FROM saved_books " +
            "WHERE status = 'TO_READ' " +
            "ORDER BY title COLLATE NOCASE ASC"
    )
    fun observeToRead(): Flow<List<SavedBookEntity>>

    @Query(
        "SELECT * FROM saved_books " +
            "WHERE status = 'ABANDONED' " +
            "ORDER BY title COLLATE NOCASE ASC"
    )
    fun observeAbandoned(): Flow<List<SavedBookEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: SavedBookEntity)

    @Query(
        "UPDATE saved_books " +
            "SET status = :status, " +
            "readingProgressPercent = :readingProgressPercent, " +
            "finishedAtEpochMillis = :finishedAtEpochMillis " +
            "WHERE `key` = :bookKey"
    )
    suspend fun updateReadingState(
        bookKey: String,
        status: String,
        readingProgressPercent: Int,
        finishedAtEpochMillis: Long?
    )

    @Query(
        "UPDATE saved_books " +
            "SET isFavorite = :isFavorite " +
            "WHERE `key` = :bookKey"
    )
    suspend fun updateFavorite(
        bookKey: String,
        isFavorite: Boolean
    )

    @Query("DELETE FROM saved_books WHERE `key` = :bookKey")
    suspend fun deleteByKey(bookKey: String)
}
