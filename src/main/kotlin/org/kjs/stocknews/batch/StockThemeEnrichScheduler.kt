package org.kjs.stocknews.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// [배치] stockThemeEnrichJob을 정해진 주기(stock.theme-enrich.cron)로 트리거하는 스케줄러. 이미 실행 중이면 스킵.
// 자동 트리거 비활성화 상태 - 아래 run() 주석 참고.
@Component
class StockThemeEnrichScheduler(
    private val jobOperator: JobOperator,
    private val jobRepository: JobRepository,
    @Qualifier("stockThemeEnrichJob") private val stockThemeEnrichJob: Job,
) {
    private val log = LoggerFactory.getLogger(StockThemeEnrichScheduler::class.java)

    // 종목 목록 API가 내려주는 업종 라벨을 TB_STOCK.THEME(SEC SIC 기반 StockTheme enum)에서
    // TB_STOCK_DETAIL.INDUSTRY_NAME(네이버 기업개요 한글 산업명)으로 교체하면서 자동 트리거를 비활성화했다.
    // SIC 분류가 실제 업종과 어긋나는 종목이 많았고(예: NKE/PG -> MATERIALS, WMT -> CONSUMER_DISCRETIONARY,
    // GOOGL -> IT), 이제 THEME 값은 어떤 응답에도 쓰이지 않아 2분마다 SEC를 호출할 이유가 없다.
    // 산업명 수집은 StockDetailEnrichScheduler가 담당한다. 코드는 되돌릴 필요가 생길 때를 위해 남겨둔다.
    // @Scheduled(cron = "\${stock.theme-enrich.cron}", zone = "Asia/Seoul")
    fun run() {
        if (jobRepository.findRunningJobExecutions("stockThemeEnrichJob").isNotEmpty()) {
            log.info("stockThemeEnrichJob already running, skipping this trigger")
            return
        }
        val jobParameters = JobParametersBuilder()
            .addLong("triggeredAt", System.currentTimeMillis())
            .toJobParameters()
        val execution = jobOperator.start(stockThemeEnrichJob, jobParameters)
        log.info("scheduled stockThemeEnrichJob started jobExecutionId={}", execution.id)
    }
}
