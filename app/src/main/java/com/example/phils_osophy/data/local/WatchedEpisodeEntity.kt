package com.example.phils_osophy.data.local

import androidx.room.Entity

@Entity(
    tableName = "watched_episodes",
    primaryKeys = ["seriesId", "seasonNumber", "episodeNumber"]
)
data class WatchedEpisodeEntity(
    val seriesId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val watchedAtEpochMillis: Long = System.currentTimeMillis()
)

data class WatchedEpisodeKey(
    val seasonNumber: Int,
    val episodeNumber: Int
)
