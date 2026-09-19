package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.StockNews
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface StockNewsRepository : JpaRepository<StockNews, Long> {
    fun existsByStockIdAndUrl(stockId: Long, url: String): Boolean

    fun deleteByCollectedAtBefore(threshold: LocalDateTime): Long

    fun findByStockIdOrderByCollectedAtDesc(stockId: Long, pageable: Pageable): Page<StockNews>

    // NewsEmbeddingJobConfig가 임베딩 미전송 건을 오래된 순으로 꺼낸다(조건 1개라 JPA 메서드 쿼리로 충분).
    fun findByEmbeddedAtIsNullOrderByIdAsc(pageable: Pageable): List<StockNews>
}
