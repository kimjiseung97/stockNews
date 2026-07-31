package org.kjs.stocknews.batch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.kjs.stocknews.model.table.User
import org.kjs.stocknews.repository.StockRepository
import org.kjs.stocknews.repository.UserRepository
import org.kjs.stocknews.repository.UserStockRepository
import org.kjs.stocknews.service.NaverNewsClient
import org.kjs.stocknews.service.NewsClient
import org.kjs.stocknews.service.NewsMailSender
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.batch.core.repository.JobRepository
import org.springframework.transaction.PlatformTransactionManager
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val USER_COUNT = 500
private const val THREAD_POOL_SIZE = 8

class NewsDispatchJobConfigTest {
    private fun newConfig(): NewsDispatchJobConfig =
        NewsDispatchJobConfig(
            jobRepository = mock(JobRepository::class.java),
            transactionManager = mock(PlatformTransactionManager::class.java),
            userRepository = mock(UserRepository::class.java),
            userStockRepository = mock(UserStockRepository::class.java),
            stockRepository = mock(StockRepository::class.java),
            newsClient = mock(NewsClient::class.java),
            naverNewsClient = mock(NaverNewsClient::class.java),
            newsMailSender = mock(NewsMailSender::class.java),
            threadPoolSize = THREAD_POOL_SIZE,
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
        val config = newConfig()
        val users = (1..USER_COUNT).map { User(email = "user$it@example.com", password = "encoded") }
        val backingRepository = mock(UserRepository::class.java)
        `when`(backingRepository.findAllByActiveTrue()).thenReturn(users)

        val configWithUsers = NewsDispatchJobConfig(
            jobRepository = mock(JobRepository::class.java),
            transactionManager = mock(PlatformTransactionManager::class.java),
            userRepository = backingRepository,
            userStockRepository = mock(UserStockRepository::class.java),
            stockRepository = mock(StockRepository::class.java),
            newsClient = mock(NewsClient::class.java),
            naverNewsClient = mock(NaverNewsClient::class.java),
            newsMailSender = mock(NewsMailSender::class.java),
            threadPoolSize = THREAD_POOL_SIZE,
        )
        val reader = configWithUsers.newsDispatchReader()

        val collected = Collections.synchronizedList(mutableListOf<User>())
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
}
