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
@Component
class StockThemeEnrichScheduler(
    private val jobOperator: JobOperator,
    private val jobRepository: JobRepository,
    @Qualifier("stockThemeEnrichJob") private val stockThemeEnrichJob: Job,
) {
    private val log = LoggerFactory.getLogger(StockThemeEnrichScheduler::class.java)

    @Scheduled(cron = "\${stock.theme-enrich.cron}", zone = "Asia/Seoul")
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
