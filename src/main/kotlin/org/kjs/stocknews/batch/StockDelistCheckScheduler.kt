package org.kjs.stocknews.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// [배치] stockDelistCheckJob을 정해진 주기(stock.delist-check.cron)로 트리거하는 스케줄러. 이미 실행 중이면 스킵.
@Component
class StockDelistCheckScheduler(
    private val jobOperator: JobOperator,
    private val jobRepository: JobRepository,
    @Qualifier("stockDelistCheckJob") private val stockDelistCheckJob: Job,
) {
    private val log = LoggerFactory.getLogger(StockDelistCheckScheduler::class.java)

    @Scheduled(cron = "\${stock.delist-check.cron}", zone = "Asia/Seoul")
    fun run() {
        if (jobRepository.findRunningJobExecutions("stockDelistCheckJob").isNotEmpty()) {
            log.info("stockDelistCheckJob already running, skipping this trigger")
            return
        }
        val jobParameters = JobParametersBuilder()
            .addLong("triggeredAt", System.currentTimeMillis())
            .toJobParameters()
        val execution = jobOperator.start(stockDelistCheckJob, jobParameters)
        log.info("scheduled stockDelistCheckJob started jobExecutionId={}", execution.id)
    }
}
