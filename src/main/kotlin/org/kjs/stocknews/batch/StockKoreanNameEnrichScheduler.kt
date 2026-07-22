package org.kjs.stocknews.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class StockKoreanNameEnrichScheduler(
    private val jobOperator: JobOperator,
    private val jobRepository: JobRepository,
    @Qualifier("stockKoreanNameEnrichJob") private val stockKoreanNameEnrichJob: Job,
) {
    private val log = LoggerFactory.getLogger(StockKoreanNameEnrichScheduler::class.java)

    @Scheduled(cron = "\${stock.korean-name-enrich.cron}")
    fun run() {
        if (jobRepository.findRunningJobExecutions("stockKoreanNameEnrichJob").isNotEmpty()) {
            log.info("stockKoreanNameEnrichJob already running, skipping this trigger")
            return
        }
        val jobParameters = JobParametersBuilder()
            .addLong("triggeredAt", System.currentTimeMillis())
            .toJobParameters()
        val execution = jobOperator.start(stockKoreanNameEnrichJob, jobParameters)
        log.info("scheduled stockKoreanNameEnrichJob started jobExecutionId={}", execution.id)
    }
}
