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
