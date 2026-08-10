package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.StockDetail
import org.springframework.data.jpa.repository.JpaRepository

interface StockDetailRepository : JpaRepository<StockDetail, Long> {
    fun findByStockId(stockId: Long): StockDetail?
}
