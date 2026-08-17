package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.StockNews
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface StockNewsRepository : JpaRepository<StockNews, Long> {
    fun existsByStockIdAndUrl(stockId: Long, url: String): Boolean

    fun deleteByCollectedAtBefore(threshold: LocalDateTime): Long
}
