package org.kjs.stocknews.batch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.kjs.stocknews.model.dto.NewsArticle
import org.kjs.stocknews.model.table.Stock
import org.kjs.stocknews.model.table.StockStatus
import org.kjs.stocknews.repository.StockNewsRepository
import org.kjs.stocknews.repository.StockRepository
import org.kjs.stocknews.service.NaverNewsClient
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.batch.core.repository.JobRepository
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.PlatformTransactionManager
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val THREAD_POOL_SIZE = 8
private const val STOCK_COUNT = 500

class StockNewsCollectJobConfigTest {
    private fun newConfig(
        naverNewsClient: NaverNewsClient = mock(NaverNewsClient::class.java),
        stockRepository: StockRepository = mock(StockRepository::class.java),
        stockNewsRepository: StockNewsRepository = mock(StockNewsRepository::class.java),
    ): StockNewsCollectJobConfig =
        StockNewsCollectJobConfig(
            jobRepository = mock(JobRepository::class.java),
            transactionManager = mock(PlatformTransactionManager::class.java),
            naverNewsClient = naverNewsClient,
            stockRepository = stockRepository,
            stockNewsRepository = stockNewsRepository,
            threadPoolSize = THREAD_POOL_SIZE,
        )

    private fun Stock.setTestId(id: Long) {
        val field = Stock::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(this, id)
    }

    @Test
    fun `stockNewsCollectReader는 ACTIVE 종목만 한 번씩 반환한다`() {
        val stockRepository = mock(StockRepository::class.java)
        val activeStock = Stock(ticker = "AAPL", name = "Apple").apply { setTestId(1L) }
        `when`(stockRepository.findByStatus(StockStatus.ACTIVE)).thenReturn(listOf(activeStock))

        val reader = newConfig(stockRepository = stockRepository).stockNewsCollectReader()

        assertEquals(activeStock, reader.read())
        assertNull(reader.read())
    }

    @Test
    fun `stockNewsCollectTaskExecutor는 설정된 스레드풀 크기로 병렬 수집을 지원한다`() {
        val executor = newConfig().stockNewsCollectTaskExecutor()

        assert(executor is ThreadPoolTaskExecutor)
        val threadPoolTaskExecutor = executor as ThreadPoolTaskExecutor
        assertEquals(THREAD_POOL_SIZE, threadPoolTaskExecutor.corePoolSize)
        assertEquals(THREAD_POOL_SIZE, threadPoolTaskExecutor.maxPoolSize)
    }

    @Test
    fun `stockNewsCollectReader는 여러 워커 스레드가 동시에 읽어도 종목을 중복이나 누락 없이 정확히 한 번씩 반환한다`() {
        val stockRepository = mock(StockRepository::class.java)
        val stocks = (1..STOCK_COUNT).map { Stock(ticker = "T$it", name = "Stock$it").apply { setTestId(it.toLong()) } }
        `when`(stockRepository.findByStatus(StockStatus.ACTIVE)).thenReturn(stocks)

        val reader = newConfig(stockRepository = stockRepository).stockNewsCollectReader()

        val collected = Collections.synchronizedList(mutableListOf<Stock>())
        val readyLatch = CountDownLatch(THREAD_POOL_SIZE)
        val startLatch = CountDownLatch(1)
        val executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE)

        repeat(THREAD_POOL_SIZE) {
            executorService.submit {
                readyLatch.countDown()
                startLatch.await()
                while (true) {
                    val stock = reader.read() ?: break
                    collected.add(stock)
                }
            }
        }

        readyLatch.await()
        startLatch.countDown()
        executorService.shutdown()
        assert(executorService.awaitTermination(10, TimeUnit.SECONDS))

        assertEquals(STOCK_COUNT, collected.size)
        assertEquals(STOCK_COUNT, collected.distinct().size)
        assertEquals(stocks.toSet(), collected.toSet())
    }

    @Test
    fun `stockNewsCollectProcessor는 이미 적재된 URL은 제외하고 신규 기사만 StockNews로 변환한다`() {
        val stock = Stock(ticker = "AAPL", name = "Apple", koreanName = "애플").apply { setTestId(1L) }

        val naverNewsClient = mock(NaverNewsClient::class.java)
        `when`(naverNewsClient.fetchNews("애플")).thenReturn(
            listOf(
                NewsArticle(title = "이미 있는 기사", url = "https://example.com/old", description = "old"),
                NewsArticle(title = "신규 기사", url = "https://example.com/new", description = "new"),
            ),
        )

        val stockNewsRepository = mock(StockNewsRepository::class.java)
        `when`(stockNewsRepository.existsByStockIdAndUrl(1L, "https://example.com/old")).thenReturn(true)
        `when`(stockNewsRepository.existsByStockIdAndUrl(1L, "https://example.com/new")).thenReturn(false)

        val processor = newConfig(
            naverNewsClient = naverNewsClient,
            stockNewsRepository = stockNewsRepository,
        ).stockNewsCollectProcessor()

        val result = processor.process(stock)

        assertEquals(1, result?.size)
        assertEquals("신규 기사", result?.first()?.title)
        assertEquals("https://example.com/new", result?.first()?.url)
        assertEquals(1L, result?.first()?.stockId)
    }

    @Test
    fun `stockNewsCollectProcessor는 신규 기사가 없으면 null을 반환한다`() {
        val stock = Stock(ticker = "AAPL", name = "Apple", koreanName = "애플").apply { setTestId(1L) }

        val naverNewsClient = mock(NaverNewsClient::class.java)
        `when`(naverNewsClient.fetchNews("애플")).thenReturn(
            listOf(NewsArticle(title = "이미 있는 기사", url = "https://example.com/old")),
        )

        val stockNewsRepository = mock(StockNewsRepository::class.java)
        `when`(stockNewsRepository.existsByStockIdAndUrl(1L, "https://example.com/old")).thenReturn(true)

        val processor = newConfig(
            naverNewsClient = naverNewsClient,
            stockNewsRepository = stockNewsRepository,
        ).stockNewsCollectProcessor()

        assertNull(processor.process(stock))
    }
}
