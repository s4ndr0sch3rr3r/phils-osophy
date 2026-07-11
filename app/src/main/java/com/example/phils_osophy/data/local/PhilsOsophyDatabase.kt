package com.example.phils_osophy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SavedMovieEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PhilsOsophyDatabase : RoomDatabase() {

    abstract fun savedMovieDao(): SavedMovieDao

    companion object {
        @Volatile
        private var instance: PhilsOsophyDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE saved_movies " +
                        "ADD COLUMN addedAtEpochMillis " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "UPDATE saved_movies " +
                        "SET addedAtEpochMillis = " +
                        "CAST(strftime('%s', 'now') AS INTEGER) * 1000 " +
                        "WHERE addedAtEpochMillis = 0"
                )
            }
        }

        fun getInstance(context: Context): PhilsOsophyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PhilsOsophyDatabase::class.java,
                    "phils_osophy.db"
                )
                    .addMigrations(migration1To2)
                    .build()
                    .also { database ->
                        instance = database
                    }
            }
    }
}
