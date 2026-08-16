package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.StockSearchCount
import org.springframework.data.jpa.repository.JpaRepository

interface StockSearchCountRepository : JpaRepository<StockSearchCount, Long>
