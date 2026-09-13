package org.kjs.stocknews.service

import org.kjs.stocknews.model.table.MailDispatchLog
import org.kjs.stocknews.model.table.MailDispatchStatus
import org.kjs.stocknews.repository.MailDispatchLogRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionException
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalTime

// ERROR_MESSAGE 컬럼 길이(500)에 맞춰 자른다.
private const val ERROR_MESSAGE_MAX_LENGTH = 500

// 다이제스트 메일 발송 결과를 TB_MAIL_DISPATCH_LOG에 남긴다(어드민 발송현황 화면의 데이터 출처).
//
// REQUIRES_NEW인 이유: 발송에 실패한 유저는 청크 트랜잭션이 롤백된 뒤 건별로 재처리되는데,
// 로그가 같은 트랜잭션에 묶여 있으면 "실패했다"는 기록까지 같이 롤백돼 사라진다.
// 대신 롤백되지 않는 만큼 재처리 때 중복 insert가 시도될 수 있어, 슬롯 유니크 제약으로 막고
// 걸린 경우는 이미 기록된 것으로 보고 무시한다.
//
// @Transactional 대신 TransactionTemplate을 쓰는 이유: 제약 위반은 트랜잭션을 rollback-only로 표시하므로
// 메서드 안에서 잡아도 커밋 시점에 UnexpectedRollbackException이 다시 튀어나온다.
// 트랜잭션 경계 '바깥'에서 잡아야 발송 흐름에 예외가 새지 않는다.
@Service
class MailDispatchLogService(
    private val mailDispatchLogRepository: MailDispatchLogRepository,
    transactionManager: PlatformTransactionManager,
) {
    private val log = LoggerFactory.getLogger(MailDispatchLogService::class.java)

    private val requiresNew = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun record(
        userId: Long,
        email: String,
        dispatchTime: LocalTime,
        status: MailDispatchStatus,
        stockCount: Int,
        articleCount: Int,
        errorMessage: String? = null,
    ) {
        val dispatchDate = LocalDate.now()
        val message = truncate(errorMessage)
        try {
            requiresNew.executeWithoutResult {
                mailDispatchLogRepository.save(
                    MailDispatchLog(
                        userId = userId,
                        email = email,
                        dispatchDate = dispatchDate,
                        dispatchTime = dispatchTime,
                        status = status,
                        stockCount = stockCount,
                        articleCount = articleCount,
                        errorMessage = message,
                    ),
                )
            }
        } catch (e: DataIntegrityViolationException) {
            // 같은 슬롯에 이미 기록이 있다 = 청크 재처리로 두 번째 시도.
            // 먼저 남은 FAILED가 그대로 굳어버리지 않도록 마지막 결과로 덮어쓴다.
            log.debug("이미 기록된 발송 로그를 덮어씁니다. userId={}, dispatchTime={}", userId, dispatchTime, e)
            overwrite(userId, dispatchDate, dispatchTime, status, stockCount, articleCount, message)
        } catch (e: DataAccessException) {
            // 통계용 기록이 메일 발송 자체를 실패시키면 안 된다 - 경고만 남기고 넘어간다.
            log.warn("발송 로그 적재 실패. userId={}, status={}", userId, status, e)
        } catch (e: TransactionException) {
            // 커밋 실패(제약 위반이 커밋 시점에 드러난 경우 포함)도 같은 이유로 삼킨다.
            log.warn("발송 로그 트랜잭션 처리 실패. userId={}, status={}", userId, status, e)
        }
    }

    // insert가 유니크 제약에 걸렸을 때만 호출된다. 실패한 트랜잭션과 분리된 새 트랜잭션에서 기존 행을 갱신한다.
    private fun overwrite(
        userId: Long,
        dispatchDate: LocalDate,
        dispatchTime: LocalTime,
        status: MailDispatchStatus,
        stockCount: Int,
        articleCount: Int,
        errorMessage: String?,
    ) {
        try {
            requiresNew.executeWithoutResult {
                val existing = mailDispatchLogRepository
                    .findByUserIdAndDispatchDateAndDispatchTime(userId, dispatchDate, dispatchTime)
                if (existing == null) {
                    log.warn("덮어쓸 발송 로그를 찾지 못했습니다. userId={}, dispatchTime={}", userId, dispatchTime)
                    return@executeWithoutResult
                }
                existing.overwriteWith(status, stockCount, articleCount, errorMessage)
            }
        } catch (e: DataAccessException) {
            log.warn("발송 로그 갱신 실패. userId={}, status={}", userId, status, e)
        } catch (e: TransactionException) {
            log.warn("발송 로그 갱신 트랜잭션 처리 실패. userId={}, status={}", userId, status, e)
        }
    }

    private fun truncate(errorMessage: String?): String? {
        if (errorMessage == null) {
            return null
        }
        if (errorMessage.length <= ERROR_MESSAGE_MAX_LENGTH) {
            return errorMessage
        }
        return errorMessage.take(ERROR_MESSAGE_MAX_LENGTH)
    }
}
