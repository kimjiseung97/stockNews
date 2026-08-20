package org.kjs.stocknews.model.dto

data class UserStockNewsView(
    val userId: Long,
    val stockId: Long,
    val ticker: String,
    val name: String,
    val koreanName: String?,
)
