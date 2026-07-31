package org.kjs.stocknews.batch

import org.kjs.stocknews.model.table.Stock
import org.kjs.stocknews.repository.StockRepository
import org.kjs.stocknews.service.SecCompanyProfileClient
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
import org.springframework.transaction.PlatformTransactionManager
import org.slf4j.LoggerFactory

private const val ENRICH_CHUNK_SIZE = 20

// [배치] theme이 비어있는 종목을 대상으로 SEC 기업 프로필의 SIC 코드를 조회해 StockTheme으로 매핑, 채운다.
// 청크 스텝(20건) - SIC 매핑 규칙은 SicThemeMapper 참고, 미해결/실패 건은 다음 배치 실행 때 재시도된다.
@Configuration
class StockThemeEnrichJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val secCompanyProfileClient: SecCompanyProfileClient,
    private val stockRepository: StockRepository,
    @Value("\${stock.theme-enrich.batch-size}") private val enrichBatchSize: Int,
) {
    private val log = LoggerFactory.getLogger(StockThemeEnrichJobConfig::class.java)

    @Bean
    fun stockThemeEnrichJob(stockThemeEnrichStep: Step): Job =
        JobBuilder("stockThemeEnrichJob", jobRepository)
            .start(stockThemeEnrichStep)
            .build()

    @Bean
    @StepScope
    fun stockThemeEnrichReader(): ItemReader<Stock> {
        val candidates = stockRepository.findByThemeIsNull(enrichBatchSize).iterator()
        return ItemReader {
            if (candidates.hasNext()) {
                candidates.next()
            }
            else null
        }
    }

    @Bean
    fun stockThemeEnrichProcessor(): ItemProcessor<Stock, Stock> = ItemProcessor { stock ->
        try {
            val profile = secCompanyProfileClient.fetchProfile(stock.cik)
            val theme = profile?.sic?.toIntOrNull()?.let { SicThemeMapper.themeForSic(it) }
            log.info("{} ({}) -> {}", stock.ticker, profile?.sicDescription, theme ?: "unresolved")
            if (theme != null) {
                stock.theme = theme
                stock
            } else {
                null
            }
        } catch (e: Exception) {
            log.warn("{} -> failed: {}", stock.ticker, e.message)
            null
        }
    }

    @Bean
    fun stockThemeEnrichWriter(): ItemWriter<Stock> = ItemWriter { stocks ->
        stockRepository.saveAll(stocks.items)
    }

    @Bean
    fun stockThemeEnrichStep(
        stockThemeEnrichReader: ItemReader<Stock>,
        stockThemeEnrichProcessor: ItemProcessor<Stock, Stock>,
        stockThemeEnrichWriter: ItemWriter<Stock>,
    ): Step =
        StepBuilder("stockThemeEnrichStep", jobRepository)
            .chunk<Stock, Stock>(ENRICH_CHUNK_SIZE)
            .reader(stockThemeEnrichReader)
            .processor(stockThemeEnrichProcessor)
            .writer(stockThemeEnrichWriter)
            .transactionManager(transactionManager)
            .build()
}
