package com.example.phils_osophy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_stats_cache")
data class ProfileStatsCacheEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val movieMinutes: Long = 0,
    val seriesMinutes: Long = 0,
    val gameMinutes: Long = 0,
    val movieCount: Int = 0,
    val watchedEpisodeCount: Int = 0,
    val gameCount: Int = 0,
    val bookCount: Int = 0,
    val finishedBookCount: Int = 0,
    val calculatedAtEpochMillis: Long = 0
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
