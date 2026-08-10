package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverStockSearchResponse(
    val items: List<NaverStockSearchItem>?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverStockSearchItem(
    val code: String?,
    val name: String?,
    val category: String?,
    val reutersCode: String?,
)
