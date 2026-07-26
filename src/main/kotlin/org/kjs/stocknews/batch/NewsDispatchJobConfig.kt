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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

private const val NEWS_DISPATCH_CHUNK_SIZE = 20

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
) {

    @Bean
    fun newsDispatchJob(newsDispatchStep: Step): Job =
        JobBuilder("newsDispatchJob", jobRepository)
            .start(newsDispatchStep)
            .build()

    @Bean
    @StepScope
    fun newsDispatchReader(): ItemReader<User> {
        val users = userRepository.findAllByActiveTrue().iterator()
        return ItemReader {
            if (users.hasNext()) users.next() else null
        }
    }

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
    ): Step =
        StepBuilder("newsDispatchStep", jobRepository)
            .chunk<User, UserNewsMail>(NEWS_DISPATCH_CHUNK_SIZE)
            .reader(newsDispatchReader)
            .processor(newsDispatchProcessor)
            .writer(newsDispatchWriter)
            .transactionManager(transactionManager)
            .build()
}
