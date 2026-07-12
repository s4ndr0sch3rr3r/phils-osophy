package com.example.phils_osophy.data.local

import android.content.Context
import com.example.phils_osophy.data.remote.TmdbClient

class SeriesCompletionTracker(
    context: Context
) {
    private val watchedEpisodeDao = PhilsOsophyDatabase
        .getInstance(context.applicationContext)
        .watchedEpisodeDao()

    suspend fun markAllEpisodesWatched(seriesId: Int) {
        val details = runCatching {
            TmdbClient.api.getSeriesDetails(seriesId)
        }.getOrNull() ?: return

        val watchedEpisodes = details.seasons
            .asSequence()
            .filter { season ->
                season.seasonNumber > 0 &&
                    season.episodeCount > 0
            }
            .flatMap { season ->
                (1..season.episodeCount).asSequence().map { episodeNumber ->
                    WatchedEpisodeEntity(
                        seriesId = seriesId,
                        seasonNumber = season.seasonNumber,
                        episodeNumber = episodeNumber
                    )
                }
            }
            .toList()

        if (watchedEpisodes.isNotEmpty()) {
            watchedEpisodeDao.markWatched(watchedEpisodes)
        }
    }
}
