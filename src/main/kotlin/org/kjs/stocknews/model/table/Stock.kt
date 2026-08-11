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
import java.time.LocalDateTime

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

    @Column(name = "KOREAN_NAME", nullable = true, length = 100)
    var koreanName: String? = null,

    // 상세정보 보강 배치가 이 종목을 마지막으로 시도한 시각(성공/실패 무관). null이면 한 번도 시도 안 한 것.
    // 실패한 종목이 계속 재시도 우선순위 맨 앞을 차지하는 걸 막기 위한 용도 - findWithoutDetail 참고.
    @Column(name = "DETAIL_ATTEMPTED_AT", nullable = true)
    var detailAttemptedAt: LocalDateTime? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Long? = null
        protected set
}
