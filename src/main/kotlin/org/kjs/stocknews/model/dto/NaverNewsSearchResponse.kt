package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverNewsSearchResponse(
    val items: List<NaverNewsItem>?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverNewsItem(
    val title: String?,
    val link: String?,
)
