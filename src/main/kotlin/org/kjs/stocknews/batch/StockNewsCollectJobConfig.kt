package org.kjs.stocknews.batch

import org.kjs.stocknews.model.table.Stock
import org.kjs.stocknews.model.table.StockNews
import org.kjs.stocknews.model.table.StockStatus
import org.kjs.stocknews.repository.StockNewsRepository
import org.kjs.stocknews.repository.StockRepository
import org.kjs.stocknews.service.NaverNewsClient
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.PlatformTransactionManager

private const val NEWS_COLLECT_CHUNK_SIZE = 20

// [배치] ACTIVE 종목 전체를 대상으로 네이버에서 뉴스를 조회해 TB_STOCK_NEWS에 적재한다.
// 같은 기사가 재수집되는 것을 막기 위해 (STOCK_ID, URL) 기준으로 이미 있는 기사는 건너뛴다.
// 멀티스레드 청크 스텝 - 종목을 20개씩 청크로 묶어 스레드풀(stockNewsCollectTaskExecutor)에서 병렬 처리(뉴스 발송 배치와 동일 패턴).
// 보관 정책(7일)은 StockNewsCleanupScheduler가 별도로 처리.
@Configuration
class StockNewsCollectJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val naverNewsClient: NaverNewsClient,
    private val stockRepository: StockRepository,
    private val stockNewsRepository: StockNewsRepository,
    @Value("\${stock.news-collect.thread-pool-size:8}") private val threadPoolSize: Int,
) {
    private val log = LoggerFactory.getLogger(StockNewsCollectJobConfig::class.java)

    // 반환 타입을 AsyncTaskExecutor로 선언하면 Spring Boot의 JPA 부트스트랩 executor 자동 감지(Map<String, AsyncTaskExecutor> 주입)에
    // 이 빈이 걸려 entityManagerFactory 생성 전에 이 설정 클래스를 조기 초기화하면서 순환 참조가 발생한다(NewsDispatchJobConfig와 동일한 이유).
    // TaskExecutor로 선언해 그 자동 감지를 피하고, 실제 사용처(stockNewsCollectStep)에서 AsyncTaskExecutor로 캐스팅한다.
    @Bean
    fun stockNewsCollectTaskExecutor(): TaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = threadPoolSize
            maxPoolSize = threadPoolSize
            setQueueCapacity(500)
            setThreadNamePrefix("stock-news-collect-")
            setWaitForTasksToCompleteOnShutdown(true)
            initialize()
        }

    // Job: stockNewsCollectStep 단일 스텝으로 구성된 종목 뉴스 수집 배치 잡.
    @Bean
    fun stockNewsCollectJob(stockNewsCollectStep: Step): Job =
        JobBuilder("stockNewsCollectJob", jobRepository)
            .start(stockNewsCollectStep)
            .build()

    // Reader: 상장폐지되지 않은(ACTIVE) 종목 전체를 한 건씩 꺼낸다.
    // 멀티스레드 스텝에서 여러 워커 스레드가 동시에 read()를 호출하므로 iterator 접근을 동기화한다.
    @Bean
    @StepScope
    fun stockNewsCollectReader(): ItemReader<Stock> {
        val stocks = stockRepository.findByStatus(StockStatus.ACTIVE).iterator()
        val lock = Any()
        return ItemReader {
            synchronized(lock) {
                if (stocks.hasNext()) stocks.next() else null
            }
        }
    }

    // Processor: 종목의 한글명(없으면 영문명)으로 네이버 뉴스를 조회해, 이미 적재된 기사(URL 중복)는 제외하고 신규 기사만 StockNews로 변환한다.
    @Bean
    fun stockNewsCollectProcessor(): ItemProcessor<Stock, List<StockNews>> = ItemProcessor { stock ->
        val stockId = stock.id ?: return@ItemProcessor null
        try {
            val articles = naverNewsClient.fetchNews(stock.koreanName ?: stock.name)
            val newArticles = articles.filterNot { stockNewsRepository.existsByStockIdAndUrl(stockId, it.url) }
            if (newArticles.isEmpty()) {
                null
            } else {
                newArticles.map { article ->
                    StockNews(stockId = stockId, title = article.title, content = article.description, url = article.url)
                }
            }
        } catch (e: Exception) {
            log.warn("{} -> news collect failed: {}", stock.ticker, e.message)
            null
        }
    }

    // Writer: 청크로 모인 종목별 신규 기사 목록을 평탄화해 그대로 저장한다.
    @Bean
    fun stockNewsCollectWriter(): ItemWriter<List<StockNews>> = ItemWriter { chunks ->
        stockNewsRepository.saveAll(chunks.items.flatten())
    }

    @Bean
    fun stockNewsCollectStep(
        stockNewsCollectReader: ItemReader<Stock>,
        stockNewsCollectProcessor: ItemProcessor<Stock, List<StockNews>>,
        stockNewsCollectWriter: ItemWriter<List<StockNews>>,
        stockNewsCollectTaskExecutor: TaskExecutor,
    ): Step =
        StepBuilder("stockNewsCollectStep", jobRepository)
            .chunk<Stock, List<StockNews>>(NEWS_COLLECT_CHUNK_SIZE)
            .reader(stockNewsCollectReader)
            .processor(stockNewsCollectProcessor)
            .writer(stockNewsCollectWriter)
            .transactionManager(transactionManager)
            .taskExecutor(stockNewsCollectTaskExecutor as AsyncTaskExecutor)
            .build()
}
