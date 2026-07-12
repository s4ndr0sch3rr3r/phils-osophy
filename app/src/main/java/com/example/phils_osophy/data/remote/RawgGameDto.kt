package com.example.phils_osophy.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RawgGameSearchResponse(
    val results: List<RawgGameSummaryDto> = emptyList()
)

@Serializable
data class RawgGameSummaryDto(
    val id: Int,
    val name: String = "",
    @SerialName("background_image")
    val backgroundImage: String? = null
)

@Serializable
data class RawgGameDetailsDto(
    val id: Int,
    val name: String = "",
    val released: String? = null,
    @SerialName("background_image")
    val backgroundImage: String? = null,
    @SerialName("description_raw")
    val description: String = "",
    val rating: Double = 0.0,
    val metacritic: Int? = null,
    val playtime: Int = 0,
    val genres: List<RawgNamedValueDto> = emptyList(),
    val platforms: List<RawgPlatformEntryDto> = emptyList(),
    val developers: List<RawgNamedValueDto> = emptyList(),
    val publishers: List<RawgNamedValueDto> = emptyList(),
    @SerialName("esrb_rating")
    val ageRating: RawgNamedValueDto? = null
)

@Serializable
data class RawgPlatformEntryDto(
    val platform: RawgNamedValueDto = RawgNamedValueDto()
)

@Serializable
data class RawgNamedValueDto(
    val id: Int = 0,
    val name: String = ""
)
