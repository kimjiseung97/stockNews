package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.Stock
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface StockRepositoryCustom {
    fun findByThemeIsNull(limit: Int): List<Stock>
    fun findAllByIdIn(ids: List<Long>): List<Stock>
    fun findByKoreanNameIsNull(limit: Int): List<Stock>
    fun search(keyword: String?, pageable: Pageable): Page<Stock>
}
