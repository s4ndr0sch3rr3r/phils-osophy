package com.example.phils_osophy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedMovieEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PhilsOsophyDatabase : RoomDatabase() {

    abstract fun savedMovieDao(): SavedMovieDao

    companion object {
        @Volatile
        private var instance: PhilsOsophyDatabase? = null

        fun getInstance(context: Context): PhilsOsophyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PhilsOsophyDatabase::class.java,
                    "phils_osophy.db"
                ).build().also { database ->
                    instance = database
                }
            }
    }
}
