package com.example.phils_osophy.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeriesSearchResponse(
    val page: Int = 1,
    val results: List<SeriesDto> = emptyList(),
    @SerialName("total_pages")
    val totalPages: Int = 0,
    @SerialName("total_results")
    val totalResults: Int = 0
)

@Serializable
data class SeriesDto(
    val id: Int,
    val name: String = "",
    val overview: String = "",
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("first_air_date")
    val firstAirDate: String = "",
    @SerialName("vote_average")
    val voteAverage: Double = 0.0
)

@Serializable
data class SeriesDetailsDto(
    val id: Int,
    val name: String = "",
    val overview: String = "",
    val genres: List<GenreDto> = emptyList(),
    val seasons: List<SeasonSummaryDto> = emptyList(),
    @SerialName("backdrop_path")
    val backdropPath: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("first_air_date")
    val firstAirDate: String = "",
    @SerialName("number_of_seasons")
    val numberOfSeasons: Int = 0,
    @SerialName("number_of_episodes")
    val numberOfEpisodes: Int = 0,
    @SerialName("episode_run_time")
    val episodeRunTime: List<Int> = emptyList(),
    @SerialName("vote_average")
    val voteAverage: Double = 0.0,
    val status: String = ""
)

@Serializable
data class SeasonSummaryDto(
    val id: Int,
    val name: String = "",
    @SerialName("air_date")
    val airDate: String? = null,
    @SerialName("episode_count")
    val episodeCount: Int = 0,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("season_number")
    val seasonNumber: Int = 0
)

@Serializable
data class SeasonDetailsDto(
    val id: Int,
    val name: String = "",
    val overview: String = "",
    @SerialName("season_number")
    val seasonNumber: Int = 0,
    val episodes: List<EpisodeDto> = emptyList()
)

@Serializable
data class EpisodeDto(
    val id: Int,
    val name: String = "",
    val overview: String = "",
    @SerialName("air_date")
    val airDate: String? = null,
    @SerialName("episode_number")
    val episodeNumber: Int = 0,
    @SerialName("season_number")
    val seasonNumber: Int = 0,
    @SerialName("still_path")
    val stillPath: String? = null,
    val runtime: Int? = null,
    @SerialName("vote_average")
    val voteAverage: Double = 0.0
)
