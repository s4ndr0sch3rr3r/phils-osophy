package com.example.phils_osophy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProfileStatsCacheDao {

    @Query("SELECT * FROM profile_stats_cache WHERE id = 1 LIMIT 1")
    suspend fun getSnapshot(): ProfileStatsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceSnapshot(snapshot: ProfileStatsCacheEntity)
}
