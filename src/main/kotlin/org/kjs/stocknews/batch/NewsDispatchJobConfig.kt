package org.kjs.stocknews.batch

import org.kjs.stocknews.model.dto.EligibleMailUserView
import org.kjs.stocknews.model.dto.NewsArticle
import org.kjs.stocknews.model.dto.UserNewsMail
import org.kjs.stocknews.model.dto.UserStockNewsView
import org.kjs.stocknews.repository.StockNewsRepository
import org.kjs.stocknews.repository.UserMailSendSettingRepository
import org.kjs.stocknews.repository.UserStockRepository
import org.kjs.stocknews.service.NewsClient
import org.kjs.stocknews.service.NewsMailSender
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
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

private const val NEWS_DISPATCH_CHUNK_SIZE = 20
private const val DISPATCH_SLOT_MINUTES = 30

// 발송시간대는 30분 단위(09:00, 09:30, ...)로만 입력받고 스케줄러도 정확히 그 간격으로 이 잡을 트리거하므로,
// cron 오차로 초/밀리초가 조금 밀려도 발송시간대 값과 정확히 일치시킬 수 있도록 30분 단위로 내림한다.
private fun currentDispatchTime(): LocalTime {
    val now = LocalTime.now()
    val slotMinute = (now.minute / DISPATCH_SLOT_MINUTES) * DISPATCH_SLOT_MINUTES
    return LocalTime.of(now.hour, slotMinute)
}

// [배치] 지금 시각(30분 단위)이 발송시간대와 정확히 일치하고 발송여부가 true인 유저의 관심종목 뉴스를
// TB_STOCK_NEWS(StockNewsCollectJobConfig가 적재)에서 모아 다이제스트 메일로 발송한다.
// 멀티스레드 청크 스텝 - 유저를 20명씩 청크로 묶어 스레드풀(newsDispatchTaskExecutor)에서 병렬 처리.
@Configuration
class NewsDispatchJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val userMailSendSettingRepository: UserMailSendSettingRepository,
    private val userStockRepository: UserStockRepository,
    private val newsClient: NewsClient,
    private val stockNewsRepository: StockNewsRepository,
    private val newsMailSender: NewsMailSender,
    @Value("\${news.dispatch.thread-pool-size:8}") private val threadPoolSize: Int,
    @Value("\${news.dispatch.max-articles-per-stock:1}") private val maxArticlesPerStock: Int,
) {

    // 반환 타입을 AsyncTaskExecutor로 선언하면 Spring Boot의 JPA 부트스트랩 executor 자동 감지(Map<String, AsyncTaskExecutor> 주입)에
    // 이 빈이 걸려 entityManagerFactory 생성 전에 이 설정 클래스를 조기 초기화하면서 순환 참조가 발생한다 (jobRepository -> transactionManager -> entityManagerFactory).
    // TaskExecutor로 선언해 그 자동 감지를 피하고, 실제 사용처(newsDispatchStep)에서 AsyncTaskExecutor로 캐스팅한다.
    @Bean
    fun newsDispatchTaskExecutor(): TaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = threadPoolSize
            maxPoolSize = threadPoolSize
            // 큐 용량 0(SynchronousQueue)은 스레드가 모두 사용 중일 때 청크 제출을 즉시 거부(TaskRejectedException)한다.
            // 청크가 몰릴 때 스레드가 빌 때까지 대기하도록 넉넉한 큐를 둔다.
            setQueueCapacity(500)
            setThreadNamePrefix("news-dispatch-")
            setWaitForTasksToCompleteOnShutdown(true)
            initialize()
        }

    // Job: newsDispatchStep 단일 스텝으로 구성된 뉴스 발송 배치 잡.
    @Bean
    fun newsDispatchJob(newsDispatchStep: Step): Job =
        JobBuilder("newsDispatchJob", jobRepository)
            .start(newsDispatchStep)
            .build()

    // Reader: 이번 발송 슬롯에 해당하는(발송여부 true + 발송시간대 일치) 유저를 한 명씩 순서대로 꺼낸다.
    @Bean
    @StepScope
    fun newsDispatchReader(): ItemReader<EligibleMailUserView> {
        val users = userMailSendSettingRepository.findEligibleUsersByDispatchTime(currentDispatchTime()).iterator()
        val lock = Any()
        return ItemReader {
            // 멀티스레드 스텝에서 여러 워커 스레드가 동시에 read()를 호출하므로 iterator 접근을 동기화한다.
            synchronized(lock) {
                if (users.hasNext()) {
                    users.next()
                } else {
                    null
                }
            }
        }
    }

    // Processor: 유저의 관심종목별로 TB_STOCK_NEWS(StockNewsCollectJobConfig가 적재한 데이터)에서
    // 최신 뉴스를 조회해 모으고, 발송할 메일 DTO(UserNewsMail)로 변환한다.
    // 관심종목이 없거나 뉴스가 하나도 없으면 null을 반환해 해당 유저를 발송 대상에서 제외한다.
    //
    // 유저마다 findAllByUserId + findAllById를 개별 호출하던 N+1 쿼리를 없애기 위해,
    // 스텝 시작 시점(@StepScope 빈 생성 1회)에 이번 슬롯 대상 유저 전체의 관심종목을 userStock-stock join 쿼리 1번으로 가져와
    // userId -> 종목 목록 Map으로 그룹핑해둔다. 이후 각 유저 처리 시점엔 DB 호출 없이 이 Map에서 꺼내 쓰기만 한다.
    //
    // 같은 종목을 구독한 유저가 여러 명이면 종목별 뉴스 조회 결과를 stockId 기준으로 캐싱해,
    // 동일 종목에 대한 TB_STOCK_NEWS 조회를 유저 수만큼 반복하지 않고 한 번으로 줄인다.
    // 멀티스레드 스텝에서 여러 워커 스레드가 동시에 접근하므로 ConcurrentHashMap을 사용한다.
    // 종목별 뉴스를 조회해 캐시에 채워두고, 이미 조회한 종목이면 캐시에서 바로 꺼내 반환한다.
    private fun newsForStock(
        stockId: Long,
        pageable: PageRequest,
        newsCache: ConcurrentHashMap<Long, List<NewsArticle>>,
    ): List<NewsArticle> =
        newsCache.computeIfAbsent(stockId) {
            stockNewsRepository.findByStockIdOrderByCollectedAtDesc(stockId, pageable)
                .content
                .map { stockNews -> NewsArticle(title = stockNews.title, url = stockNews.url, description = stockNews.content) }
        }

    @Bean
    @StepScope
    fun newsDispatchProcessor(): ItemProcessor<EligibleMailUserView, UserNewsMail> {
        val eligibleUserIds = userMailSendSettingRepository.findEligibleUsersByDispatchTime(currentDispatchTime()).map { it.userId }
        val stockViewsByUserId: Map<Long, List<UserStockNewsView>> = userStockRepository.findNewsViewsByUserIdIn(eligibleUserIds).groupBy { it.userId }
        val newsCache = ConcurrentHashMap<Long, List<NewsArticle>>()
        val pageable = PageRequest.of(0, maxArticlesPerStock)

        return ItemProcessor { it ->
            val stockViews = stockViewsByUserId.get(it.userId)
            if (stockViews.isNullOrEmpty()) {
                return@ItemProcessor null
            }

            val articlesByTicker = linkedMapOf<String, List<NewsArticle>>()
            for (stock in stockViews) {
                val articles = newsForStock(stock.stockId, pageable, newsCache)
                if (articles.isNotEmpty()) {
                    articlesByTicker.put(stock.ticker, articles)
                }
            }

            if (articlesByTicker.isEmpty()) {
                null
            } else {
                UserNewsMail(it.email, articlesByTicker)
            }
        }
    }

    // Writer: 청크로 모인 UserNewsMail을 순회하며 실제 다이제스트 메일을 발송한다.
    @Bean
    fun newsDispatchWriter(): ItemWriter<UserNewsMail> = ItemWriter { mails ->
        for (mail in mails.items) {
            newsMailSender.sendNewsDigest(mail.email, mail.articlesByTicker)
        }
    }

    @Bean
    fun newsDispatchStep(
        newsDispatchReader: ItemReader<EligibleMailUserView>,
        newsDispatchProcessor: ItemProcessor<EligibleMailUserView, UserNewsMail>,
        newsDispatchWriter: ItemWriter<UserNewsMail>,
        newsDispatchTaskExecutor: TaskExecutor,
    ): Step =
        StepBuilder("newsDispatchStep", jobRepository)
            .chunk<EligibleMailUserView, UserNewsMail>(NEWS_DISPATCH_CHUNK_SIZE)
            .reader(newsDispatchReader)
            .processor(newsDispatchProcessor)
            .writer(newsDispatchWriter)
            .transactionManager(transactionManager)
            .taskExecutor(newsDispatchTaskExecutor as AsyncTaskExecutor)
            .build()
}
