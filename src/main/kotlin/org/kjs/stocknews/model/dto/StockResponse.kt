package org.kjs.stocknews.model.dto

data class StockResponse(
    val id: Long,
    val ticker: String,
    val name: String,
    // 네이버 기업개요에서 받은 한글 산업명(TB_STOCK_DETAIL.INDUSTRY_NAME). 기존에는 SIC 기반 StockTheme enum을
    // 내려줬으나 SIC 분류가 실제 업종과 어긋나는 종목이 많아(예: NKE/PG -> MATERIALS) 산업명으로 대체했다.
    val theme: String?,
    val koreanName: String?,
)
