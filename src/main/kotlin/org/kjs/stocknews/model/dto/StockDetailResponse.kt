package org.kjs.stocknews.model.dto

import org.kjs.stocknews.model.table.StockDetail
import java.time.LocalDateTime

data class StockDetailResponse(
    val stockId: Long,
    val summary: String?,
    val representativeName: String?,
    val nation: String?,
    val city: String?,
    val homepageUrl: String?,
    val industryName: String?,
    val listedAt: LocalDateTime?,
) {
    companion object {
        fun from(stockDetail: StockDetail): StockDetailResponse =
            StockDetailResponse(
                stockId = stockDetail.stockId,
                summary = stockDetail.summary,
                representativeName = stockDetail.representativeName,
                nation = stockDetail.nation,
                city = stockDetail.city,
                homepageUrl = stockDetail.homepageUrl,
                industryName = stockDetail.industryName,
                listedAt = stockDetail.listedAt,
            )
    }
}
