package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.Stock

interface StockRepositoryCustom {
    fun findByThemeIsNull(limit: Int): List<Stock>
}
