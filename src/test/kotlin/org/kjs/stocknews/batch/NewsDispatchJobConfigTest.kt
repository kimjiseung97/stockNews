package org.kjs.stocknews.batch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kjs.stocknews.model.dto.EligibleMailUserView
import org.kjs.stocknews.model.dto.UserStockNewsView
import org.kjs.stocknews.model.dto.NewsArticle
import org.kjs.stocknews.model.dto.UserNewsMail
import org.kjs.stocknews.model.table.MailDispatchStatus
import org.kjs.stocknews.model.table.StockNews
import org.kjs.stocknews.repository.StockNewsRepository
import org.kjs.stocknews.repository.UserMailSendSettingRepository
import org.kjs.stocknews.repository.UserStockRepository
import org.kjs.stocknews.service.MailDispatchLogService
import org.kjs.stocknews.service.NewsClient
import org.kjs.stocknews.service.NewsMailSender
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.core.repository.JobRepository
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.mail.MailSendException
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalTime
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val USER_COUNT = 500
private const val THREAD_POOL_SIZE = 8
private const val MAX_ARTICLES_PER_STOCK = 1
private val ANY_DISPATCH_TIME = LocalTime.of(9, 0)

// Mockito의 any()는 널을 반환해 Kotlin non-null 파라미터 콜사이트에서 NPE를 유발한다.
// 로컬 래퍼로 정적 반환 타입을 non-null로 감춰서 우회한다(널리 알려진 Kotlin+Mockito 관용구).
private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()

class NewsDispatchJobConfigTest {
    private fun mailSettingRepositoryReturning(users: List<EligibleMailUserView>): UserMailSendSettingRepository =
        mock(UserMailSendSettingRepository::class.java).also {
            `when`(it.findEligibleUsersByDispatchTime(anyArg())).thenReturn(users)
        }

    private fun newConfig(users: List<EligibleMailUserView> = emptyList()): NewsDispatchJobConfig =
        NewsDispatchJobConfig(
            jobRepository = mock(JobRepository::class.java),
            transactionManager = mock(PlatformTransactionManager::class.java),
            userMailSendSettingRepository = mailSettingRepositoryReturning(users),
            userStockRepository = mock(UserStockRepository::class.java),
            newsClient = mock(NewsClient::class.java),
            stockNewsRepository = mock(StockNewsRepository::class.java),
            newsMailSender = mock(NewsMailSender::class.java),
            mailDispatchLogService = mock(MailDispatchLogService::class.java),
            threadPoolSize = THREAD_POOL_SIZE,
            maxArticlesPerStock = MAX_ARTICLES_PER_STOCK,
        )

    @Test
    fun `newsDispatchTaskExecutor는 설정된 스레드풀 크기로 병렬 발송을 지원한다`() {
        val executor = newConfig().newsDispatchTaskExecutor()

        assert(executor is org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor)
        val threadPoolTaskExecutor = executor as org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
        assertEquals(THREAD_POOL_SIZE, threadPoolTaskExecutor.corePoolSize)
        assertEquals(THREAD_POOL_SIZE, threadPoolTaskExecutor.maxPoolSize)
    }

    @Test
    fun `newsDispatchReader는 여러 워커 스레드가 동시에 읽어도 유저를 중복이나 누락 없이 정확히 한 번씩 반환한다`() {
        val users = (1..USER_COUNT).map {
            EligibleMailUserView(userId = it.toLong(), email = "user$it@example.com", dispatchTime = ANY_DISPATCH_TIME)
        }
        val reader = newConfig(users).newsDispatchReader()

        val collected = Collections.synchronizedList(mutableListOf<EligibleMailUserView>())
        val readyLatch = CountDownLatch(THREAD_POOL_SIZE)
        val startLatch = CountDownLatch(1)
        val executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE)

        repeat(THREAD_POOL_SIZE) {
            executorService.submit {
                readyLatch.countDown()
                startLatch.await()
                while (true) {
                    val user = reader.read() ?: break
                    collected.add(user)
                }
            }
        }

        readyLatch.await()
        startLatch.countDown()
        executorService.shutdown()
        assert(executorService.awaitTermination(10, TimeUnit.SECONDS))

        assertEquals(USER_COUNT, collected.size)
        assertEquals(USER_COUNT, collected.distinct().size)
        assertEquals(users.toSet(), collected.toSet())
    }

    @Test
    fun `newsDispatchProcessor는 유저별 관심종목을 배치 join 결과에서 조회해 뉴스가 있는 종목만 메일로 담는다`() {
        val userA = EligibleMailUserView(userId = 1L, email = "a@example.com", dispatchTime = ANY_DISPATCH_TIME)
        val userB = EligibleMailUserView(userId = 2L, email = "b@example.com", dispatchTime = ANY_DISPATCH_TIME)

        val userStockRepository = mock(UserStockRepository::class.java)
        `when`(userStockRepository.findNewsViewsByUserIdIn(listOf(1L, 2L))).thenReturn(
            listOf(
                UserStockNewsView(userId = 1L, stockId = 10L, ticker = "AAPL", name = "Apple", koreanName = "애플"),
                UserStockNewsView(userId = 1L, stockId = 20L, ticker = "MSFT", name = "Microsoft", koreanName = null),
            ),
            // userB(2L)는 관심종목이 없는 케이스
        )

        val stockNewsRepository = mock(StockNewsRepository::class.java)
        val pageable = PageRequest.of(0, MAX_ARTICLES_PER_STOCK)
        val appleNews = StockNews(stockId = 10L, title = "애플 뉴스", content = null, url = "https://example.com/apple")
        `when`(stockNewsRepository.findByStockIdOrderByCollectedAtDesc(10L, pageable))
            .thenReturn(PageImpl(listOf(appleNews)))
        `when`(stockNewsRepository.findByStockIdOrderByCollectedAtDesc(20L, pageable))
            .thenReturn(PageImpl(emptyList()))

        val config = NewsDispatchJobConfig(
            jobRepository = mock(JobRepository::class.java),
            transactionManager = mock(PlatformTransactionManager::class.java),
            userMailSendSettingRepository = mailSettingRepositoryReturning(listOf(userA, userB)),
            userStockRepository = userStockRepository,
            newsClient = mock(NewsClient::class.java),
            stockNewsRepository = stockNewsRepository,
            newsMailSender = mock(NewsMailSender::class.java),
            mailDispatchLogService = mock(MailDispatchLogService::class.java),
            threadPoolSize = THREAD_POOL_SIZE,
            maxArticlesPerStock = MAX_ARTICLES_PER_STOCK,
        )
        val processor = config.newsDispatchProcessor()

        val mailForA = processor.process(userA)
        assertEquals("a@example.com", mailForA?.email)
        assertEquals(setOf("AAPL"), mailForA?.articlesByTicker?.keys)

        val mailForB = processor.process(userB)
        assertNull(mailForB)
    }

    // --- 발송 결과 적재(TB_MAIL_DISPATCH_LOG) ---

    private fun configWith(
        users: List<EligibleMailUserView>,
        userStockRepository: UserStockRepository = mock(UserStockRepository::class.java),
        stockNewsRepository: StockNewsRepository = mock(StockNewsRepository::class.java),
        newsMailSender: NewsMailSender = mock(NewsMailSender::class.java),
        mailDispatchLogService: MailDispatchLogService = mock(MailDispatchLogService::class.java),
    ): NewsDispatchJobConfig =
        NewsDispatchJobConfig(
            jobRepository = mock(JobRepository::class.java),
            transactionManager = mock(PlatformTransactionManager::class.java),
            userMailSendSettingRepository = mailSettingRepositoryReturning(users),
            userStockRepository = userStockRepository,
            newsClient = mock(NewsClient::class.java),
            stockNewsRepository = stockNewsRepository,
            newsMailSender = newsMailSender,
            mailDispatchLogService = mailDispatchLogService,
            threadPoolSize = THREAD_POOL_SIZE,
            maxArticlesPerStock = MAX_ARTICLES_PER_STOCK,
        )

    private fun mailFor(user: EligibleMailUserView): UserNewsMail =
        UserNewsMail(
            userId = user.userId,
            email = user.email,
            dispatchTime = user.dispatchTime,
            articlesByTicker = mapOf(
                "AAPL" to listOf(NewsArticle(title = "애플 뉴스", url = "https://example.com/apple", description = null)),
                "MSFT" to listOf(NewsArticle(title = "MS 뉴스", url = "https://example.com/ms", description = null)),
            ),
        )

    @Test
    fun `관심종목이 없는 유저는 SKIPPED로 기록된다`() {
        val user = EligibleMailUserView(userId = 1L, email = "a@example.com", dispatchTime = ANY_DISPATCH_TIME)
        val mailDispatchLogService = mock(MailDispatchLogService::class.java)
        val config = configWith(users = listOf(user), mailDispatchLogService = mailDispatchLogService)

        assertNull(config.newsDispatchProcessor().process(user))

        verify(mailDispatchLogService).record(
            userId = 1L,
            email = "a@example.com",
            dispatchTime = ANY_DISPATCH_TIME,
            status = MailDispatchStatus.SKIPPED,
            stockCount = 0,
            articleCount = 0,
            errorMessage = null,
        )
    }

    @Test
    fun `관심종목은 있지만 보낼 뉴스가 없으면 종목 수와 함께 SKIPPED로 기록된다`() {
        val user = EligibleMailUserView(userId = 1L, email = "a@example.com", dispatchTime = ANY_DISPATCH_TIME)
        val userStockRepository = mock(UserStockRepository::class.java)
        `when`(userStockRepository.findNewsViewsByUserIdIn(listOf(1L))).thenReturn(
            listOf(
                UserStockNewsView(userId = 1L, stockId = 10L, ticker = "AAPL", name = "Apple", koreanName = "애플"),
                UserStockNewsView(userId = 1L, stockId = 20L, ticker = "MSFT", name = "Microsoft", koreanName = null),
            ),
        )
        val stockNewsRepository = mock(StockNewsRepository::class.java)
        val pageable = PageRequest.of(0, MAX_ARTICLES_PER_STOCK)
        `when`(stockNewsRepository.findByStockIdOrderByCollectedAtDesc(10L, pageable)).thenReturn(PageImpl(emptyList()))
        `when`(stockNewsRepository.findByStockIdOrderByCollectedAtDesc(20L, pageable)).thenReturn(PageImpl(emptyList()))
        val mailDispatchLogService = mock(MailDispatchLogService::class.java)
        val config = configWith(
            users = listOf(user),
            userStockRepository = userStockRepository,
            stockNewsRepository = stockNewsRepository,
            mailDispatchLogService = mailDispatchLogService,
        )

        assertNull(config.newsDispatchProcessor().process(user))

        verify(mailDispatchLogService).record(
            userId = 1L,
            email = "a@example.com",
            dispatchTime = ANY_DISPATCH_TIME,
            status = MailDispatchStatus.SKIPPED,
            stockCount = 2,
            articleCount = 0,
            errorMessage = null,
        )
    }

    @Test
    fun `발송에 성공하면 종목 수와 기사 수를 담아 SUCCESS로 기록된다`() {
        val user = EligibleMailUserView(userId = 7L, email = "a@example.com", dispatchTime = ANY_DISPATCH_TIME)
        val mailDispatchLogService = mock(MailDispatchLogService::class.java)
        val config = configWith(users = listOf(user), mailDispatchLogService = mailDispatchLogService)

        config.newsDispatchWriter().write(Chunk(listOf(mailFor(user))))

        verify(mailDispatchLogService).record(
            userId = 7L,
            email = "a@example.com",
            dispatchTime = ANY_DISPATCH_TIME,
            status = MailDispatchStatus.SUCCESS,
            stockCount = 2,
            articleCount = 2,
            errorMessage = null,
        )
    }

    @Test
    fun `발송에 실패하면 FAILED로 기록하고 예외를 그대로 던져 스텝의 skip 정책에 맡긴다`() {
        val user = EligibleMailUserView(userId = 7L, email = "a@example.com", dispatchTime = ANY_DISPATCH_TIME)
        val newsMailSender = mock(NewsMailSender::class.java)
        doThrow(MailSendException("smtp down")).`when`(newsMailSender).sendNewsDigest(anyArg(), anyArg())
        val mailDispatchLogService = mock(MailDispatchLogService::class.java)
        val config = configWith(
            users = listOf(user),
            newsMailSender = newsMailSender,
            mailDispatchLogService = mailDispatchLogService,
        )

        assertThrows<MailSendException> { config.newsDispatchWriter().write(Chunk(listOf(mailFor(user)))) }

        verify(mailDispatchLogService).record(
            userId = 7L,
            email = "a@example.com",
            dispatchTime = ANY_DISPATCH_TIME,
            status = MailDispatchStatus.FAILED,
            stockCount = 2,
            articleCount = 2,
            errorMessage = "smtp down",
        )
    }
}
