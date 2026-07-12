package com.example.phils_osophy.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): MovieSearchResponse

    @GET("search/tv")
    suspend fun searchSeries(
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = "fr-FR",
        @Query("page") page: Int = 1
    ): SeriesSearchResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "en-US"
    ): MovieDetailsDto
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

@Serializable
data class MovieDetailsDto(
    val id: Int,
    val title: String = "",
    val overview: String = "",
    val runtime: Int? = null,
    val genres: List<GenreDto> = emptyList(),

    @SerialName("backdrop_path")
    val backdropPath: String? = null,

    @SerialName("poster_path")
    val posterPath: String? = null,

    @SerialName("release_date")
    val releaseDate: String = ""
)

@Serializable
data class GenreDto(
    val id: Int,
    val name: String = ""
)
