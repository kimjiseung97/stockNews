package org.kjs.stocknews.batch

import org.kjs.stocknews.model.dto.NewsArticle
import org.kjs.stocknews.model.dto.UserNewsMail
import org.kjs.stocknews.model.table.User
import org.kjs.stocknews.repository.StockRepository
import org.kjs.stocknews.repository.UserRepository
import org.kjs.stocknews.repository.UserStockRepository
import org.kjs.stocknews.service.NaverNewsClient
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.PlatformTransactionManager

private const val NEWS_DISPATCH_CHUNK_SIZE = 20

// [배치] 활성 유저별 관심종목 뉴스를 모아 다이제스트 메일로 발송한다.
// 멀티스레드 청크 스텝 - 유저를 20명씩 청크로 묶어 스레드풀(newsDispatchTaskExecutor)에서 병렬 처리.
@Configuration
class NewsDispatchJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val userRepository: UserRepository,
    private val userStockRepository: UserStockRepository,
    private val stockRepository: StockRepository,
    private val newsClient: NewsClient,
    private val naverNewsClient: NaverNewsClient,
    private val newsMailSender: NewsMailSender,
    @Value("\${news.dispatch.thread-pool-size:8}") private val threadPoolSize: Int,
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

    // Reader: 활성 유저를 한 명씩 순서대로 꺼낸다.
    @Bean
    @StepScope
    fun newsDispatchReader(): ItemReader<User> {
        val users = userRepository.findAllByActiveTrue().iterator()
        val lock = Any()
        return ItemReader {
            // 멀티스레드 스텝에서 여러 워커 스레드가 동시에 read()를 호출하므로 iterator 접근을 동기화한다.
            synchronized(lock) {
                if (users.hasNext()) users.next() else null
            }
        }
    }

    // Processor: 유저의 관심종목별로 네이버 뉴스를 조회해 모으고, 발송할 메일 DTO(UserNewsMail)로 변환한다.
    // 관심종목이 없거나 뉴스가 하나도 없으면 null을 반환해 해당 유저를 발송 대상에서 제외한다.
    @Bean
    @StepScope
    fun newsDispatchProcessor(): ItemProcessor<User, UserNewsMail> = ItemProcessor { user ->
        val userId = user.id ?: return@ItemProcessor null

        val userStocks = userStockRepository.findAllByUserId(userId)
        if (userStocks.isEmpty()) return@ItemProcessor null

        val stockIds = mutableListOf<Long>()
        for (userStock in userStocks) {
            stockIds.add(userStock.stockId)
        }
        val stocks = stockRepository.findAllById(stockIds)

        val articlesByTicker = linkedMapOf<String, List<NewsArticle>>()
        for (stock in stocks) {
            val articles = naverNewsClient.fetchNews(stock.koreanName ?: stock.name)
            if (articles.isNotEmpty()) {
                articlesByTicker[stock.ticker] = articles
            }
        }

        if (articlesByTicker.isEmpty()) {
            null
        } else {
            UserNewsMail(user.email, articlesByTicker)
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
        newsDispatchReader: ItemReader<User>,
        newsDispatchProcessor: ItemProcessor<User, UserNewsMail>,
        newsDispatchWriter: ItemWriter<UserNewsMail>,
        newsDispatchTaskExecutor: TaskExecutor,
    ): Step =
        StepBuilder("newsDispatchStep", jobRepository)
            .chunk<User, UserNewsMail>(NEWS_DISPATCH_CHUNK_SIZE)
            .reader(newsDispatchReader)
            .processor(newsDispatchProcessor)
            .writer(newsDispatchWriter)
            .transactionManager(transactionManager)
            .taskExecutor(newsDispatchTaskExecutor as AsyncTaskExecutor)
            .build()
}
