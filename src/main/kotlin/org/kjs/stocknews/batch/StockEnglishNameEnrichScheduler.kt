package org.kjs.stocknews.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// [배치] stockEnglishNameEnrichJob을 정해진 주기(stock.english-name-enrich.cron)로 트리거하는 스케줄러. 이미 실행 중이면 스킵.
@Component
class StockEnglishNameEnrichScheduler(
    private val jobOperator: JobOperator,
    private val jobRepository: JobRepository,
    @Qualifier("stockEnglishNameEnrichJob") private val stockEnglishNameEnrichJob: Job,
) {
    private val log = LoggerFactory.getLogger(StockEnglishNameEnrichScheduler::class.java)

    @Scheduled(cron = "\${stock.english-name-enrich.cron}", zone = "Asia/Seoul")
    fun run() {
        if (jobRepository.findRunningJobExecutions("stockEnglishNameEnrichJob").isNotEmpty()) {
            log.info("stockEnglishNameEnrichJob already running, skipping this trigger")
            return
        }
        val jobParameters = JobParametersBuilder()
            .addLong("triggeredAt", System.currentTimeMillis())
            .toJobParameters()
        val execution = jobOperator.start(stockEnglishNameEnrichJob, jobParameters)
        log.info("scheduled stockEnglishNameEnrichJob started jobExecutionId={}", execution.id)
    }
}
