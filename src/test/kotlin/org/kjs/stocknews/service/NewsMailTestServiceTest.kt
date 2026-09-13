package org.kjs.stocknews.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.NewsArticle
import org.kjs.stocknews.model.dto.UserStockNewsView
import org.kjs.stocknews.model.table.StockNews
import org.kjs.stocknews.model.table.User
import org.kjs.stocknews.repository.StockNewsRepository
import org.kjs.stocknews.repository.UserRepository
import org.kjs.stocknews.repository.UserStockRepository
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.mail.MailSendException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional

// Mockito의 any()는 null을 반환해 Kotlin의 non-null 파라미터에서 NPE가 난다.
// 매처 등록만 시키고 값은 캐스팅으로 통과시키는 관용 우회.
@Suppress("UNCHECKED_CAST")
private fun <T> uninitialized(): T = null as T

private fun <T> anyArg(): T = ArgumentMatchers.any<T>() ?: uninitialized()

// eq()도 null을 반환하는데, Kotlin은 non-null 파라미터 자리에서 그 null을 즉시 걸러버린다.
private fun eqArg(value: String): String = eq(value) ?: value

// Clock.fixed는 시각이 고정돼 "쿨다운이 지나면 다시 된다"를 검증할 수 없어 직접 진전시키는 시계를 쓴다.
private class MutableClock(var now: Instant) : Clock() {
    override fun getZone(): ZoneOffset = ZoneOffset.UTC
    override fun withZone(zone: java.time.ZoneId): Clock = this
    override fun instant(): Instant = now
}

class NewsMailTestServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val userStockRepository = mock(UserStockRepository::class.java)
    private val stockNewsRepository = mock(StockNewsRepository::class.java)
    private val newsMailSender = mock(NewsMailSender::class.java)

    private val baseInstant: Instant = Instant.parse("2026-09-13T02:00:00Z")

    private fun service(clock: Clock = Clock.fixed(baseInstant, ZoneOffset.UTC)) =
        NewsMailTestService(userRepository, userStockRepository, stockNewsRepository, newsMailSender, 1, clock)

    private fun user(email: String = "me@example.com") = User(email = email, password = "encoded")

    private fun stockView(stockId: Long, ticker: String) =
        UserStockNewsView(userId = 1L, stockId = stockId, ticker = ticker, name = ticker, koreanName = null)

    private fun news(stockId: Long, title: String) =
        StockNews(stockId = stockId, title = title, content = "요약", url = "https://news/$title")

    @Test
    fun `관심종목 뉴스가 있으면 본인 이메일로 발송하고 건수를 반환한다`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user()))
        `when`(userStockRepository.findNewsViewsByUserIdIn(listOf(1L))).thenReturn(listOf(stockView(10L, "AAPL")))
        `when`(stockNewsRepository.findByStockIdOrderByCollectedAtDesc(eq(10L), anyArg<Pageable>()))
            .thenReturn(PageImpl(listOf(news(10L, "애플 뉴스"))))

        val response = service().sendTestDigest(1L)

        assert(response.email == "me@example.com")
        assert(response.stockCount == 1)
        assert(response.articleCount == 1)
        verify(newsMailSender).sendNewsDigest(eqArg("me@example.com"), anyMap())
    }

    @Test
    fun `관심종목이 하나도 없으면 NEWS_MAIL_NO_STOCKS로 실패한다`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user()))
        `when`(userStockRepository.findNewsViewsByUserIdIn(listOf(1L))).thenReturn(emptyList())

        val e = assertThrows<BusinessException> { service().sendTestDigest(1L) }

        assert(e.resultCode == ResultCode.NEWS_MAIL_NO_STOCKS)
        verify(newsMailSender, never()).sendNewsDigest(anyString(), anyMap())
    }

    @Test
    fun `관심종목은 있지만 뉴스가 하나도 없으면 NEWS_MAIL_NO_ARTICLES로 실패한다`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user()))
        `when`(userStockRepository.findNewsViewsByUserIdIn(listOf(1L))).thenReturn(listOf(stockView(10L, "AAPL")))
        `when`(stockNewsRepository.findByStockIdOrderByCollectedAtDesc(anyLong(), anyArg<Pageable>()))
            .thenReturn(PageImpl(emptyList<StockNews>()))

        val e = assertThrows<BusinessException> { service().sendTestDigest(1L) }

        assert(e.resultCode == ResultCode.NEWS_MAIL_NO_ARTICLES)
        verify(newsMailSender, never()).sendNewsDigest(anyString(), anyMap())
    }

    @Test
    fun `SMTP 발송이 실패하면 NEWS_MAIL_SEND_FAILED로 변환한다`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user()))
        `when`(userStockRepository.findNewsViewsByUserIdIn(listOf(1L))).thenReturn(listOf(stockView(10L, "AAPL")))
        `when`(stockNewsRepository.findByStockIdOrderByCollectedAtDesc(anyLong(), anyArg<Pageable>()))
            .thenReturn(PageImpl(listOf(news(10L, "애플 뉴스"))))
        doThrow(MailSendException("smtp down"))
            .`when`(newsMailSender).sendNewsDigest(anyString(), anyMap())

        val e = assertThrows<BusinessException> { service().sendTestDigest(1L) }

        assert(e.resultCode == ResultCode.NEWS_MAIL_SEND_FAILED)
    }

    @Test
    fun `관심종목이 없어 발송 자체를 못 한 경우엔 쿨다운을 소모하지 않는다`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user()))
        `when`(userStockRepository.findNewsViewsByUserIdIn(listOf(1L))).thenReturn(emptyList())
        `when`(stockNewsRepository.findByStockIdOrderByCollectedAtDesc(anyLong(), anyArg<Pageable>()))
            .thenReturn(PageImpl(listOf(news(10L, "애플 뉴스"))))

        val clock = MutableClock(baseInstant)
        val service = service(clock)

        // 첫 호출은 관심종목이 없어 실패한다.
        assertThrows<BusinessException> { service.sendTestDigest(1L) }
        clock.now = baseInstant.plusSeconds(5)

        // 관심종목을 등록한 직후(= 쿨다운 안)에 다시 시도해도 막히지 않아야 한다.
        `when`(userStockRepository.findNewsViewsByUserIdIn(listOf(1L))).thenReturn(listOf(stockView(10L, "AAPL")))
        val response = service.sendTestDigest(1L)

        assert(response.articleCount == 1)
    }

    @Test
    fun `SMTP 발송 실패는 쿨다운을 소모한다`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user()))
        `when`(userStockRepository.findNewsViewsByUserIdIn(listOf(1L))).thenReturn(listOf(stockView(10L, "AAPL")))
        `when`(stockNewsRepository.findByStockIdOrderByCollectedAtDesc(anyLong(), anyArg<Pageable>()))
            .thenReturn(PageImpl(listOf(news(10L, "애플 뉴스"))))
        doThrow(MailSendException("smtp down")).`when`(newsMailSender).sendNewsDigest(anyString(), anyMap())

        val clock = MutableClock(baseInstant)
        val service = service(clock)

        assertThrows<BusinessException> { service.sendTestDigest(1L) }
        clock.now = baseInstant.plusSeconds(5)
        val second = assertThrows<BusinessException> { service.sendTestDigest(1L) }

        assert(second.resultCode == ResultCode.NEWS_MAIL_TEST_TOO_FREQUENT)
    }

    @Test
    fun `쿨다운 안에 다시 호출하면 NEWS_MAIL_TEST_TOO_FREQUENT로 막는다`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user()))
        `when`(userStockRepository.findNewsViewsByUserIdIn(listOf(1L))).thenReturn(listOf(stockView(10L, "AAPL")))
        `when`(stockNewsRepository.findByStockIdOrderByCollectedAtDesc(anyLong(), anyArg<Pageable>()))
            .thenReturn(PageImpl(listOf(news(10L, "애플 뉴스"))))

        // 같은 서비스 인스턴스가 쿨다운 상태를 들고 있어야 하므로 시계를 30초만 진전시킨다.
        val clock = MutableClock(baseInstant)
        val service = service(clock)

        service.sendTestDigest(1L)
        clock.now = baseInstant.plusSeconds(30)
        val e = assertThrows<BusinessException> { service.sendTestDigest(1L) }

        assert(e.resultCode == ResultCode.NEWS_MAIL_TEST_TOO_FREQUENT)

        // 쿨다운이 지나면 다시 발송된다.
        clock.now = baseInstant.plusSeconds(61)
        service.sendTestDigest(1L)
        verify(newsMailSender, times(2)).sendNewsDigest(anyString(), anyMap())
    }
}
