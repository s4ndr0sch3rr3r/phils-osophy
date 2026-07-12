package com.example.phils_osophy.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_games")
data class SavedGameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val hoursPlayedMinutes: Int,
    @ColumnInfo(defaultValue = "1")
    val playerCount: Int,
    @ColumnInfo(defaultValue = "0")
    val isFinished: Boolean,
    @ColumnInfo(defaultValue = "0")
    val addedAtEpochMillis: Long = System.currentTimeMillis()
)
