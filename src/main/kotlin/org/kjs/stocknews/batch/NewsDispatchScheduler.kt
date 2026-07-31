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
class NewsDispatchScheduler(
    private val jobOperator: JobOperator,
    private val jobRepository: JobRepository,
    @Qualifier("newsDispatchJob") private val newsDispatchJob: Job,
) {
    private val log = LoggerFactory.getLogger(NewsDispatchScheduler::class.java)

    @Scheduled(cron = "\${news.dispatch.cron}", zone = "Asia/Seoul")
    fun run() {
        if (jobRepository.findRunningJobExecutions("newsDispatchJob").isNotEmpty()) {
            log.info("newsDispatchJob already running, skipping this trigger")
            return
        }
        val jobParameters = JobParametersBuilder()
            .addLong("triggeredAt", System.currentTimeMillis())
            .toJobParameters()
        val execution = jobOperator.start(newsDispatchJob, jobParameters)
        log.info("scheduled newsDispatchJob started jobExecutionId={}", execution.id)
    }
}
