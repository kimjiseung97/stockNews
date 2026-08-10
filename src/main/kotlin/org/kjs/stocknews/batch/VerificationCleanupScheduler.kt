package org.kjs.stocknews.batch

import org.kjs.stocknews.repository.EmailVerificationRepository
import org.kjs.stocknews.repository.VerificationRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

// [배치] TB_EMAIL_VERIFICATION, TB_VERIFICATION의 만료(EXPIRES_AT 경과) 레코드를 주기적으로 삭제하는 경량 스케줄러.
@Component
class VerificationCleanupScheduler(
    private val emailVerificationRepository: EmailVerificationRepository,
    private val verificationRepository: VerificationRepository,
) {
    private val log = LoggerFactory.getLogger(VerificationCleanupScheduler::class.java)

    @Transactional
    @Scheduled(cron = "\${verification.cleanup.cron}", zone = "Asia/Seoul")
    fun cleanup() {
        val now = LocalDateTime.now()
        val emailDeleted = emailVerificationRepository.deleteByExpiresAtBefore(now)
        val verificationDeleted = verificationRepository.deleteByExpiresAtBefore(now)
        log.info(
            "verification cleanup done: emailVerificationDeleted={}, verificationDeleted={}",
            emailDeleted,
            verificationDeleted,
        )
    }
}
