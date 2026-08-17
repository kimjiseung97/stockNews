package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

// 종목별 뉴스 수집 배치(StockNewsCollectJobConfig)가 적재하는 테이블. 웹사이트 종목별 뉴스 탭 조회용으로
// 7일 보관 후 StockNewsCleanupScheduler가 삭제한다. STOCK_ID는 TB_STOCK.ID를 논리적으로 참조(물리 FK 없음, 프로젝트 컨벤션).
@Entity
@Table(
    name = "TB_STOCK_NEWS",
    indexes = [
        Index(name = "IDX_TB_STOCK_NEWS_STOCK_ID", columnList = "STOCK_ID"),
        // StockNewsCleanupScheduler의 deleteByCollectedAtBefore가 이 컬럼으로 스캔하므로 인덱스 필요.
        Index(name = "IDX_TB_STOCK_NEWS_COLLECTED_AT", columnList = "COLLECTED_AT"),
    ],
    uniqueConstraints = [UniqueConstraint(name = "UK_TB_STOCK_NEWS_STOCK_URL", columnNames = ["STOCK_ID", "URL"])],
)
class StockNews(
    @Column(name = "STOCK_ID", nullable = false)
    val stockId: Long,

    @Column(name = "TITLE", nullable = false, length = 500)
    val title: String,

    // 네이버 뉴스검색 API의 description은 전체 본문이 아니라 짧은 요약 스니펫이라 4000이면 충분히 여유 있음.
    @Column(name = "CONTENT", nullable = true, length = 4000)
    val content: String?,

    @Column(name = "URL", nullable = false, length = 500)
    val url: String,

    @Column(name = "COLLECTED_AT", nullable = false, updatable = false)
    val collectedAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Long? = null
        protected set
}
