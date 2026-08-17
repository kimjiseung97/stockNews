package org.kjs.stocknews.batch

import org.kjs.stocknews.repository.StockNewsRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

// [배치] TB_STOCK_NEWS의 보관 기간(stock.news-cleanup.retention-days, 기본 7일)을 지난 기사를 주기적으로 삭제하는 경량 스케줄러.
@Component
class StockNewsCleanupScheduler(
    private val stockNewsRepository: StockNewsRepository,
    @Value("\${stock.news-cleanup.retention-days:7}") private val retentionDays: Long,
) {
    private val log = LoggerFactory.getLogger(StockNewsCleanupScheduler::class.java)

    @Transactional
    @Scheduled(cron = "\${stock.news-cleanup.cron}", zone = "Asia/Seoul")
    fun cleanup() {
        val threshold = LocalDateTime.now().minusDays(retentionDays)
        val deleted = stockNewsRepository.deleteByCollectedAtBefore(threshold)
        log.info("stock news cleanup done: deleted={}, retentionDays={}", deleted, retentionDays)
    }
}
