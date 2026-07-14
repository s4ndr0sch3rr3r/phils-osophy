package com.example.phils_osophy.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "saved_recipes")
data class SavedRecipeEntity(
    @PrimaryKey
    val key: String,
    val title: String,
    val status: String,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val addedAtEpochMillis: Long = System.currentTimeMillis()
)

enum class RecipeStatus {
    IN_PROGRESS,
    FINISHED,
    TO_TRY,
    STOPPED;

    companion object {
        fun fromStorage(value: String): RecipeStatus =
            RecipeStatus.values().firstOrNull { status ->
                status.name == value
            } ?: TO_TRY
    }
}

fun createSavedRecipeEntity(
    title: String,
    status: RecipeStatus,
    addedAtEpochMillis: Long = System.currentTimeMillis(),
    isFavorite: Boolean = false
): SavedRecipeEntity {
    val cleanTitle = title.trim().replace(Regex("\\s+"), " ")
    return SavedRecipeEntity(
        key = cleanTitle.lowercase(Locale.ROOT),
        title = cleanTitle,
        status = status.name,
        isFavorite = isFavorite,
        addedAtEpochMillis = addedAtEpochMillis
    )
}
