package org.kjs.stocknews.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

// [배치] 성공/실패 여부와 상관없이 생성된 지 retention-days(기본 30일) 지난 배치 실행 이력을 정리한다.
// 2~10분 주기로 도는 스케줄러들이 트리거될 때마다 새 JobInstance를 만들기 때문에 방치하면
// BATCH_JOB_INSTANCE/BATCH_JOB_EXECUTION/BATCH_STEP_EXECUTION 등이 무한정 쌓인다.
// deleteJobInstance()가 하위 JobExecution/StepExecution과 각각의 ExecutionContext, JobExecutionParams까지
// 함께 삭제해준다(JobRepository 자체 API, raw SQL 없음).
@Component
class BatchHistoryCleanupScheduler(
    private val jobRepository: JobRepository,
    @Value("\${stock.batch-history-cleanup.retention-days}") private val retentionDays: Long,
) {
    private val log = LoggerFactory.getLogger(BatchHistoryCleanupScheduler::class.java)

    @Scheduled(cron = "\${stock.batch-history-cleanup.cron}", zone = "Asia/Seoul")
    fun cleanup() {
        val threshold = LocalDateTime.now().minusDays(retentionDays)
        var deletedCount = 0

        jobRepository.jobNames.forEach { jobName ->
            jobRepository.findJobInstances(jobName).forEach { instance ->
                val executions = jobRepository.getJobExecutions(instance)
                val isRunning = executions.any { it.status == BatchStatus.STARTED || it.status == BatchStatus.STARTING }
                val isExpired = executions.all { it.createTime.isBefore(threshold) }

                if (!isRunning && isExpired) {
                    jobRepository.deleteJobInstance(instance)
                    deletedCount++
                }
            }
        }

        if (deletedCount > 0) {
            log.info("deleted {} batch job instance(s) older than {} day(s)", deletedCount, retentionDays)
        }
    }
}
