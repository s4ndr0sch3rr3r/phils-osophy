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
        SavedSeriesEntity::class,
        WatchedEpisodeEntity::class,
        SavedGameEntity::class,
        SavedBookEntity::class,
        SavedRecipeEntity::class,
        ProfileStatsCacheEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class PhilsOsophyDatabase : RoomDatabase() {

    abstract fun savedMovieDao(): SavedMovieDao

    abstract fun savedSeriesDao(): SavedSeriesDao

    abstract fun watchedEpisodeDao(): WatchedEpisodeDao

    abstract fun savedGameDao(): SavedGameDao

    abstract fun savedBookDao(): SavedBookDao

    abstract fun savedRecipeDao(): SavedRecipeDao

    abstract fun profileStatsCacheDao(): ProfileStatsCacheDao

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

        private val migration5To6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS watched_episodes (" +
                        "seriesId INTEGER NOT NULL, " +
                        "seasonNumber INTEGER NOT NULL, " +
                        "episodeNumber INTEGER NOT NULL, " +
                        "watchedAtEpochMillis INTEGER NOT NULL, " +
                        "PRIMARY KEY(seriesId, seasonNumber, episodeNumber))"
                )
            }
        }

        private val migration6To7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS saved_games (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "hoursPlayedMinutes INTEGER NOT NULL, " +
                        "playerCount INTEGER NOT NULL DEFAULT 1, " +
                        "isFinished INTEGER NOT NULL DEFAULT 0, " +
                        "addedAtEpochMillis INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        private val migration7To8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS saved_books (" +
                        "`key` TEXT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "authors TEXT NOT NULL, " +
                        "firstPublishYear INTEGER, " +
                        "coverId INTEGER, " +
                        "editionCount INTEGER NOT NULL, " +
                        "addedAtEpochMillis INTEGER NOT NULL DEFAULT 0, " +
                        "status TEXT NOT NULL, " +
                        "isFavorite INTEGER NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY(`key`))"
                )
            }
        }

        private val migration8To9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE saved_books " +
                        "ADD COLUMN readingProgressPercent " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE saved_books " +
                        "ADD COLUMN finishedAtEpochMillis INTEGER"
                )
                database.execSQL(
                    "UPDATE saved_books " +
                        "SET readingProgressPercent = 100 " +
                        "WHERE status = 'FINISHED'"
                )
            }
        }

        private val migration9To10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS profile_stats_cache (" +
                        "id INTEGER NOT NULL, " +
                        "movieMinutes INTEGER NOT NULL, " +
                        "seriesMinutes INTEGER NOT NULL, " +
                        "gameMinutes INTEGER NOT NULL, " +
                        "movieCount INTEGER NOT NULL, " +
                        "watchedEpisodeCount INTEGER NOT NULL, " +
                        "gameCount INTEGER NOT NULL, " +
                        "bookCount INTEGER NOT NULL, " +
                        "finishedBookCount INTEGER NOT NULL, " +
                        "calculatedAtEpochMillis INTEGER NOT NULL, " +
                        "PRIMARY KEY(id))"
                )
                database.execSQL(
                    "INSERT OR IGNORE INTO profile_stats_cache (" +
                        "id, movieMinutes, seriesMinutes, gameMinutes, " +
                        "movieCount, watchedEpisodeCount, gameCount, " +
                        "bookCount, finishedBookCount, calculatedAtEpochMillis" +
                        ") VALUES (1, 0, 0, 0, 0, 0, 0, 0, 0, 0)"
                )
            }
        }

        private val migration10To11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE saved_series ADD COLUMN userRating " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE saved_games ADD COLUMN userRating " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE saved_books ADD COLUMN userRating " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val migration11To12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE saved_series ADD COLUMN note " +
                        "TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE saved_books ADD COLUMN note " +
                        "TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val migration12To13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                listOf(
                    "saved_movies",
                    "saved_series",
                    "saved_games",
                    "saved_books"
                ).forEach { tableName ->
                    database.execSQL(
                        "UPDATE $tableName SET userRating = CASE " +
                            "WHEN userRating <= 0 THEN 0 " +
                            "WHEN userRating <= 2 THEN 1 " +
                            "WHEN userRating <= 4 THEN 2 " +
                            "WHEN userRating <= 6 THEN 3 " +
                            "WHEN userRating <= 8 THEN 4 " +
                            "ELSE 5 END"
                    )
                }
            }
        }

        private val migration13To14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS saved_recipes (" +
                        "`key` TEXT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "status TEXT NOT NULL, " +
                        "isFavorite INTEGER NOT NULL DEFAULT 0, " +
                        "addedAtEpochMillis INTEGER NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY(`key`))"
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
                        migration4To5,
                        migration5To6,
                        migration6To7,
                        migration7To8,
                        migration8To9,
                        migration9To10,
                        migration10To11,
                        migration11To12,
                        migration12To13,
                        migration13To14
                    )
                    .build()
                    .also { database ->
                        instance = database
                    }
            }
    }
}
