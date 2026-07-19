package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.Stock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface StockRepository : JpaRepository<Stock, Long>, StockRepositoryCustom {
    fun existsByTicker(ticker: String): Boolean

    @Query("select s.ticker from Stock s")
    fun findAllTickers(): List<String>
}
