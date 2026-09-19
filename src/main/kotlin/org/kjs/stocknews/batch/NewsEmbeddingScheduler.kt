package org.kjs.stocknews.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// [배치] newsEmbeddingJob을 정해진 주기(news.embedding.cron)로 트리거하는 스케줄러. 이미 실행 중이면 스킵.
// 한 실행이 임베딩 서비스의 CPU 추론을 기다리며 길어질 수 있어, 중복 실행 방지가 특히 중요하다
// (같은 뉴스를 두 실행이 동시에 집어 같은 묶음을 두 번 보내게 된다).
@Component
class NewsEmbeddingScheduler(
    private val jobOperator: JobOperator,
    private val jobRepository: JobRepository,
    @Qualifier("newsEmbeddingJob") private val newsEmbeddingJob: Job,
) {
    private val log = LoggerFactory.getLogger(NewsEmbeddingScheduler::class.java)

    @Scheduled(cron = "\${news.embedding.cron}", zone = "Asia/Seoul")
    fun run() {
        if (jobRepository.findRunningJobExecutions("newsEmbeddingJob").isNotEmpty()) {
            log.info("newsEmbeddingJob already running, skipping this trigger")
            return
        }
        val jobParameters = JobParametersBuilder()
            .addLong("triggeredAt", System.currentTimeMillis())
            .toJobParameters()
        val execution = jobOperator.start(newsEmbeddingJob, jobParameters)
        log.info("scheduled newsEmbeddingJob started jobExecutionId={}", execution.id)
    }
}
