package org.kjs.stocknews.model.dto

import org.kjs.stocknews.model.table.StockTheme

data class UserStockResponse(
    val ticker: String,
    val name: String,
    val theme: StockTheme?,
)
