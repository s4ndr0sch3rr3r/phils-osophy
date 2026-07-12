package com.example.phils_osophy.data.local

import android.content.Context
import com.example.phils_osophy.data.remote.TmdbClient

class SeriesCompletionTracker(
    context: Context
) {
    private val database = PhilsOsophyDatabase.getInstance(
        context.applicationContext
    )
    private val watchedEpisodeDao = database.watchedEpisodeDao()
    private val savedSeriesDao = database.savedSeriesDao()

    suspend fun setEpisodeWatched(
        seriesId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
        watched: Boolean
    ) {
        if (watched) {
            watchedEpisodeDao.markWatched(
                WatchedEpisodeEntity(
                    seriesId = seriesId,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber
                )
            )
            moveToInProgressIfNeeded(seriesId)
        } else {
            watchedEpisodeDao.markUnwatched(
                seriesId = seriesId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )
        }

        synchronizeStatus(seriesId)
    }

    suspend fun markSeasonWatched(
        seriesId: Int,
        seasonNumber: Int,
        episodeCount: Int
    ) {
        if (episodeCount <= 0) {
            return
        }

        watchedEpisodeDao.markWatched(
            (1..episodeCount).map { episodeNumber ->
                WatchedEpisodeEntity(
                    seriesId = seriesId,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber
                )
            }
        )

        moveToInProgressIfNeeded(seriesId)
        synchronizeStatus(seriesId)
    }

    suspend fun markAllEpisodesWatched(seriesId: Int) {
        val episodeKeys = loadExpectedEpisodes(seriesId) ?: return

        if (episodeKeys.isNotEmpty()) {
            watchedEpisodeDao.markWatched(
                episodeKeys.map { key ->
                    WatchedEpisodeEntity(
                        seriesId = seriesId,
                        seasonNumber = key.seasonNumber,
                        episodeNumber = key.episodeNumber
                    )
                }
            )
        }

        savedSeriesDao.updateStatus(
            seriesId = seriesId,
            status = SeriesStatus.FINISHED.name
        )
    }

    private suspend fun moveToInProgressIfNeeded(seriesId: Int) {
        val savedSeries = savedSeriesDao.getById(seriesId) ?: return

        if (
            SeriesStatus.fromStorage(savedSeries.status) ==
            SeriesStatus.TO_WATCH
        ) {
            savedSeriesDao.updateStatus(
                seriesId = seriesId,
                status = SeriesStatus.IN_PROGRESS.name
            )
        }
    }

    private suspend fun synchronizeStatus(seriesId: Int) {
        val expectedEpisodes = loadExpectedEpisodes(seriesId) ?: return

        if (expectedEpisodes.isEmpty()) {
            return
        }

        val watchedEpisodes = watchedEpisodeDao
            .getForSeries(seriesId)
            .map { watched ->
                WatchedEpisodeKey(
                    seasonNumber = watched.seasonNumber,
                    episodeNumber = watched.episodeNumber
                )
            }
            .toSet()
        val savedSeries = savedSeriesDao.getById(seriesId) ?: return
        val currentStatus = SeriesStatus.fromStorage(savedSeries.status)
        val allEpisodesWatched = expectedEpisodes.all { expected ->
            expected in watchedEpisodes
        }

        when {
            allEpisodesWatched && currentStatus != SeriesStatus.FINISHED -> {
                savedSeriesDao.updateStatus(
                    seriesId = seriesId,
                    status = SeriesStatus.FINISHED.name
                )
            }

            !allEpisodesWatched && currentStatus == SeriesStatus.FINISHED -> {
                savedSeriesDao.updateStatus(
                    seriesId = seriesId,
                    status = SeriesStatus.IN_PROGRESS.name
                )
            }
        }
    }

    private suspend fun loadExpectedEpisodes(
        seriesId: Int
    ): Set<WatchedEpisodeKey>? {
        val details = runCatching {
            TmdbClient.api.getSeriesDetails(seriesId)
        }.getOrNull() ?: return null

        return details.seasons
            .asSequence()
            .filter { season ->
                season.seasonNumber > 0 &&
                    season.episodeCount > 0
            }
            .flatMap { season ->
                (1..season.episodeCount).asSequence().map { episodeNumber ->
                    WatchedEpisodeKey(
                        seasonNumber = season.seasonNumber,
                        episodeNumber = episodeNumber
                    )
                }
            }
            .toSet()
    }
}
