package org.kjs.stocknews.model.dto

data class StockChatRequest(
    val stockId: Long,
    val question: String,
)
