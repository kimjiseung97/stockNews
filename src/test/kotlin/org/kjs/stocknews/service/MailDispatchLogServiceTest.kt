package org.kjs.stocknews.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.kjs.stocknews.model.table.MailDispatchLog
import org.kjs.stocknews.model.table.MailDispatchStatus
import org.kjs.stocknews.repository.MailDispatchLogRepository
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import java.time.LocalDate
import java.time.LocalTime

private fun <T> anyArg(): T = ArgumentMatchers.any()

private val DISPATCH_TIME = LocalTime.of(9, 0)

class MailDispatchLogServiceTest {
    private val mailDispatchLogRepository = mock(MailDispatchLogRepository::class.java)
    private val transactionManager = mock(PlatformTransactionManager::class.java).also {
        `when`(it.getTransaction(anyArg())).thenReturn(SimpleTransactionStatus())
    }
    private val mailDispatchLogService = MailDispatchLogService(mailDispatchLogRepository, transactionManager)

    private fun record(status: MailDispatchStatus, errorMessage: String? = null) {
        mailDispatchLogService.record(
            userId = 1L,
            email = "a@example.com",
            dispatchTime = DISPATCH_TIME,
            status = status,
            stockCount = 2,
            articleCount = 3,
            errorMessage = errorMessage,
        )
    }

    @Test
    fun `발송 결과를 그대로 적재한다`() {
        record(MailDispatchStatus.SUCCESS)

        val captor = ArgumentCaptor.forClass(MailDispatchLog::class.java)
        verify(mailDispatchLogRepository).save(captor.capture())
        val saved = captor.value
        assertEquals(1L, saved.userId)
        assertEquals("a@example.com", saved.email)
        assertEquals(LocalDate.now(), saved.dispatchDate)
        assertEquals(DISPATCH_TIME, saved.dispatchTime)
        assertEquals(MailDispatchStatus.SUCCESS, saved.status)
        assertEquals(2, saved.stockCount)
        assertEquals(3, saved.articleCount)
        assertNull(saved.errorMessage)
    }

    @Test
    fun `같은 슬롯에 기록이 이미 있으면 마지막 결과로 덮어쓴다`() {
        // 발송 실패로 FAILED가 먼저 커밋된 뒤 재처리가 성공한 상황.
        val existing = MailDispatchLog(
            userId = 1L,
            email = "a@example.com",
            dispatchDate = LocalDate.now(),
            dispatchTime = DISPATCH_TIME,
            status = MailDispatchStatus.FAILED,
            stockCount = 2,
            articleCount = 3,
            errorMessage = "smtp down",
        )
        `when`(mailDispatchLogRepository.save(anyArg<MailDispatchLog>()))
            .thenThrow(DataIntegrityViolationException("duplicate slot"))
        `when`(mailDispatchLogRepository.findByUserIdAndDispatchDateAndDispatchTime(1L, LocalDate.now(), DISPATCH_TIME))
            .thenReturn(existing)

        record(MailDispatchStatus.SUCCESS)

        assertEquals(MailDispatchStatus.SUCCESS, existing.status)
        assertNull(existing.errorMessage)
    }

    @Test
    fun `에러 메시지가 길면 컬럼 길이에 맞춰 잘라 담는다`() {
        record(MailDispatchStatus.FAILED, errorMessage = "e".repeat(600))

        val captor = ArgumentCaptor.forClass(MailDispatchLog::class.java)
        verify(mailDispatchLogRepository).save(captor.capture())
        assertEquals(500, captor.value.errorMessage?.length)
    }
}
