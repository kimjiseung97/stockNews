package org.kjs.stocknews.model.dto

import org.kjs.stocknews.model.table.Stock
import org.kjs.stocknews.model.table.StockTheme

data class StockResponse(
    val id: Long,
    val ticker: String,
    val name: String,
    val theme: StockTheme?,
    val koreanName: String?,
) {
    companion object {
        fun from(stock: Stock): StockResponse =
            StockResponse(
                id = stock.id!!,
                ticker = stock.ticker,
                name = stock.name,
                theme = stock.theme,
                koreanName = stock.koreanName,
            )
    }
}
