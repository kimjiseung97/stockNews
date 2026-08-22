package org.kjs.stocknews.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// [배치] stockNewsCollectJob을 정해진 주기(stock.news-collect.cron)로 트리거하는 스케줄러. 이미 실행 중이면 스킵.
// ACTIVE 종목 전체를 대상으로 하는 잡이라 오래 걸릴 수 있음 - 하루 1회 정도로 넉넉하게 잡는다.
@Component
class StockNewsCollectScheduler(
    private val jobOperator: JobOperator,
    private val jobRepository: JobRepository,
    @Qualifier("stockNewsCollectJob") private val stockNewsCollectJob: Job,
) {
    private val log = LoggerFactory.getLogger(StockNewsCollectScheduler::class.java)

    // stockNewsSchedule(Node) 서비스가 news-collect 배치를 대체 수행하므로 중복 수집 방지를 위해 자동 트리거 비활성화.
    // @Scheduled(cron = "\${stock.news-collect.cron}", zone = "Asia/Seoul")
    fun run() {
        if (jobRepository.findRunningJobExecutions("stockNewsCollectJob").isNotEmpty()) {
            log.info("stockNewsCollectJob already running, skipping this trigger")
            return
        }
        val jobParameters = JobParametersBuilder()
            .addLong("triggeredAt", System.currentTimeMillis())
            .toJobParameters()
        val execution = jobOperator.start(stockNewsCollectJob, jobParameters)
        log.info("scheduled stockNewsCollectJob started jobExecutionId={}", execution.id)
    }
}
