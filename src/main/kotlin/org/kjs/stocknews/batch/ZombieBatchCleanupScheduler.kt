package org.kjs.stocknews.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

// [배치] 컨테이너 재시작 등으로 STARTED/STARTING 상태에 멈춰 종료되지 못한 좀비 JobExecution을 찾아 FAILED로 정리한다.
// 좀비 레코드가 남아있으면 각 배치 스케줄러의 findRunningJobExecutions() 체크가 계속 "실행 중"으로 오인해
// 이후 트리거를 영구적으로 스킵하게 된다.
@Component
class ZombieBatchCleanupScheduler(
    private val jobRepository: JobRepository,
    @Value("\${stock.batch-watchdog.stale-minutes}") private val staleMinutes: Long,
) {
    private val log = LoggerFactory.getLogger(ZombieBatchCleanupScheduler::class.java)

    @Scheduled(cron = "\${stock.batch-watchdog.cron}", zone = "Asia/Seoul")
    fun cleanup() {
        val threshold = LocalDateTime.now().minusMinutes(staleMinutes)

        jobRepository.jobNames
            .flatMap { jobRepository.findRunningJobExecutions(it) }
            .filter { it.startTime?.isBefore(threshold) == true }
            .forEach(::markFailed)
    }

    private fun markFailed(execution: JobExecution) {
        log.warn(
            "orphaned batch execution detected jobExecutionId={} jobName={} startTime={} -> marking FAILED",
            execution.id, execution.jobInstance.jobName, execution.startTime,
        )

        val now = LocalDateTime.now()
        execution.stepExecutions.forEach { step ->
            step.status = BatchStatus.FAILED
            step.exitStatus = ExitStatus.FAILED
            step.setEndTime(now)
            jobRepository.update(step)
        }

        execution.status = BatchStatus.FAILED
        execution.exitStatus = ExitStatus.FAILED
        execution.setEndTime(now)
        jobRepository.update(execution)
    }
}
