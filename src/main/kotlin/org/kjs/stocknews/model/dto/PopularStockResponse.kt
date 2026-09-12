package org.kjs.stocknews.model.dto

data class PopularStockResponse(
    val id: Long,
    val ticker: String,
    val name: String,
    // 네이버 기업개요 기반 한글 산업명 - StockResponse.theme 주석 참고.
    val theme: String?,
    val koreanName: String?,
    val searchCount: Long,
)
