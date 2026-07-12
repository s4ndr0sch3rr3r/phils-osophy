package com.example.phils_osophy.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RawgApi {

    @GET("games")
    suspend fun searchGames(
        @Query("key") apiKey: String,
        @Query("search") query: String,
        @Query("search_precise") precise: Boolean = true,
        @Query("page_size") pageSize: Int = 5
    ): RawgGameSearchResponse

    @GET("games/{gameId}")
    suspend fun getGameDetails(
        @Path("gameId") gameId: Int,
        @Query("key") apiKey: String
    ): RawgGameDetailsDto
}
