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
    val status: String = "",
    val genres: List<GenreDto> = emptyList(),
    @SerialName("backdrop_path")
    val backdropPath: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    @SerialName("first_air_date")
    val firstAirDate: String = "",
    @SerialName("last_air_date")
    val lastAirDate: String = "",
    @SerialName("number_of_seasons")
    val numberOfSeasons: Int = 0,
    @SerialName("number_of_episodes")
    val numberOfEpisodes: Int = 0,
    @SerialName("in_production")
    val inProduction: Boolean = false,
    @SerialName("episode_run_time")
    val episodeRunTime: List<Int> = emptyList()
)
