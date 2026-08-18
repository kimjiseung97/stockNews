package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

// 종목 검색 통계용 로그 테이블 - 검색 1회당 1행 적재, 통계는 STOCK_ID로 GROUP BY해서 집계.
@Entity
@Table(name = "TB_STOCK_SEARCH_COUNT", indexes = [Index(name = "IDX_TB_STOCK_SEARCH_COUNT_STOCK_ID", columnList = "STOCK_ID")])
class StockSearchCount(
    @Column(name = "STOCK_ID", nullable = false)
    val stockId: Long,

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Long? = null
        protected set
}
