package com.example.phils_osophy.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookSearchResponse(
    val docs: List<BookDto> = emptyList()
)

@Serializable
data class BookDto(
    val key: String,
    val title: String = "",
    @SerialName("author_name")
    val authorNames: List<String> = emptyList(),
    @SerialName("first_publish_year")
    val firstPublishYear: Int? = null,
    @SerialName("cover_i")
    val coverId: Int? = null,
    @SerialName("edition_count")
    val editionCount: Int = 0
)