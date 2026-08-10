package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverStockOverviewResponse(
    val summary: String?,
    val summaries: NaverStockOverviewSummaries?,
    val industry: NaverStockOverviewIndustry?,
    val stockItemListedInfo: NaverStockOverviewListedInfo?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverStockOverviewSummaries(
    val representativeName: String?,
    val nation: String?,
    val city: String?,
    val url: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverStockOverviewIndustry(
    val industryGroupKor: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverStockOverviewListedInfo(
    val listedAt: String?,
)
