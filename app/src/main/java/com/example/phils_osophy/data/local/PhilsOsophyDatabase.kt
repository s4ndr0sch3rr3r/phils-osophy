package com.example.phils_osophy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SavedMovieEntity::class,
        SavedSeriesEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class PhilsOsophyDatabase : RoomDatabase() {

    abstract fun savedMovieDao(): SavedMovieDao

    abstract fun savedSeriesDao(): SavedSeriesDao

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

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE saved_movies " +
                        "ADD COLUMN userRating " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE saved_movies " +
                        "ADD COLUMN note " +
                        "TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE saved_movies " +
                        "ADD COLUMN isFavorite " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val migration4To5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS saved_series (" +
                        "id INTEGER NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "overview TEXT NOT NULL, " +
                        "posterPath TEXT, " +
                        "firstAirDate TEXT NOT NULL, " +
                        "voteAverage REAL NOT NULL, " +
                        "addedAtEpochMillis INTEGER NOT NULL DEFAULT 0, " +
                        "status TEXT NOT NULL, " +
                        "isFavorite INTEGER NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY(id))"
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
                    .addMigrations(
                        migration1To2,
                        migration2To3,
                        migration3To4,
                        migration4To5
                    )
                    .build()
                    .also { database ->
                        instance = database
                    }
            }
    }
}
