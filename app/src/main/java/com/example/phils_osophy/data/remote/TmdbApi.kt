package com.example.phils_osophy.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): MovieSearchResponse
}

@Serializable
data class MovieSearchResponse(
    val page: Int = 1,
    val results: List<MovieDto> = emptyList(),

    @SerialName("total_pages")
    val totalPages: Int = 0,

    @SerialName("total_results")
    val totalResults: Int = 0
)

@Serializable
data class MovieDto(
    val id: Int,
    val title: String = "",
    val overview: String = "",

    @SerialName("poster_path")
    val posterPath: String? = null,

    @SerialName("release_date")
    val releaseDate: String = "",

    @SerialName("vote_average")
    val voteAverage: Double = 0.0
)