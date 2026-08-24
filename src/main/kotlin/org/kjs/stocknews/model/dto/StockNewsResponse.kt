package org.kjs.stocknews.model.dto

import org.kjs.stocknews.model.table.StockNews
import java.time.LocalDateTime

data class StockNewsResponse(
    val id: Long,
    val title: String,
    val content: String?,
    val url: String,
    val collectedAt: LocalDateTime,
) {
    companion object {
        fun from(stockNews: StockNews): StockNewsResponse =
            StockNewsResponse(
                id = requireNotNull(stockNews.id),
                title = stockNews.title,
                content = stockNews.content,
                url = stockNews.url,
                collectedAt = stockNews.collectedAt,
            )
    }
}
