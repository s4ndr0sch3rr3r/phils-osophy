package com.example.phils_osophy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SavedMovieEntity::class],
    version = 4,
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
                        migration3To4
                    )
                    .build()
                    .also { database ->
                        instance = database
                    }
            }
    }
}
