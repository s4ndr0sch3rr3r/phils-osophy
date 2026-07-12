package com.example.phils_osophy.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.phils_osophy.data.remote.BookDto

@Entity(tableName = "saved_books")
data class SavedBookEntity(
    @PrimaryKey
    val key: String,
    val title: String,
    val authors: String,
    val firstPublishYear: Int?,
    val coverId: Int?,
    val editionCount: Int,
    @ColumnInfo(defaultValue = "0")
    val addedAtEpochMillis: Long,
    val status: String,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean,
    @ColumnInfo(defaultValue = "0")
    val readingProgressPercent: Int,
    val finishedAtEpochMillis: Long?,
    @ColumnInfo(defaultValue = "0")
    val userRating: Int
)

enum class BookStatus {
    IN_PROGRESS,
    FINISHED,
    TO_READ,
    ABANDONED;

    companion object {
        fun fromStorage(value: String): BookStatus =
            entries.firstOrNull { status ->
                status.name == value
            } ?: TO_READ
    }
}

fun BookDto.toSavedBookEntity(
    status: BookStatus,
    addedAtEpochMillis: Long = System.currentTimeMillis(),
    isFavorite: Boolean = false,
    userRating: Int = 0
): SavedBookEntity {
    val isFinished = status == BookStatus.FINISHED

    return SavedBookEntity(
        key = key,
        title = title,
        authors = authorNames.joinToString(", "),
        firstPublishYear = firstPublishYear,
        coverId = coverId,
        editionCount = editionCount,
        addedAtEpochMillis = addedAtEpochMillis,
        status = status.name,
        isFavorite = isFavorite,
        readingProgressPercent = if (isFinished) 100 else 0,
        finishedAtEpochMillis = if (isFinished) {
            System.currentTimeMillis()
        } else {
            null
        },
        userRating = userRating.coerceIn(0, 10)
    )
}
