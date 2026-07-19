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
class StockSeedScheduler(
    private val jobOperator: JobOperator,
    private val jobRepository: JobRepository,
    @Qualifier("stockSeedJob") private val stockSeedJob: Job,
) {
    private val log = LoggerFactory.getLogger(StockSeedScheduler::class.java)

    @Scheduled(cron = "\${stock.seed.cron}")
    fun run() {
        if (jobRepository.findRunningJobExecutions("stockSeedJob").isNotEmpty()) {
            log.info("stockSeedJob already running, skipping this trigger")
            return
        }
        val jobParameters = JobParametersBuilder()
            .addLong("triggeredAt", System.currentTimeMillis())
            .toJobParameters()
        val execution = jobOperator.start(stockSeedJob, jobParameters)
        log.info("scheduled stockSeedJob started jobExecutionId={}", execution.id)
    }
}
