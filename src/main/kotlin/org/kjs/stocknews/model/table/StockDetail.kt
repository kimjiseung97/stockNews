package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "TB_STOCK_DETAIL")
class StockDetail(
    @Column(name = "STOCK_ID", nullable = false, unique = true)
    val stockId: Long,

    @Column(name = "SUMMARY", nullable = true, length = 2000)
    var summary: String? = null,

    @Column(name = "REPRESENTATIVE_NAME", nullable = true, length = 100)
    var representativeName: String? = null,

    @Column(name = "NATION", nullable = true, length = 50)
    var nation: String? = null,

    @Column(name = "CITY", nullable = true, length = 100)
    var city: String? = null,

    @Column(name = "HOMEPAGE_URL", nullable = true, length = 255)
    var homepageUrl: String? = null,

    @Column(name = "INDUSTRY_NAME", nullable = true, length = 100)
    var industryName: String? = null,

    @Column(name = "LISTED_AT", nullable = true)
    var listedAt: LocalDateTime? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Long? = null
        protected set
}
