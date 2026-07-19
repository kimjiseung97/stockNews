package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(name = "TB_STOCK", indexes = [Index(name = "IDX_TB_STOCK_THEME", columnList = "THEME")])
class Stock(
    @Column(name = "TICKER", nullable = false, unique = true, length = 10)
    val ticker: String,

    @Column(name = "NAME", nullable = false, length = 100)
    var name: String,

    @Column(name = "CIK", nullable = false)
    val cik: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "THEME", nullable = true, length = 30)
    var theme: StockTheme? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Long? = null
        protected set
}
