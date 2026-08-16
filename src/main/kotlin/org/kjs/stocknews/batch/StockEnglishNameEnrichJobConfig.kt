package org.kjs.stocknews.batch

import org.kjs.stocknews.model.table.Stock
import org.kjs.stocknews.repository.StockRepository
import org.kjs.stocknews.service.FinnhubCompanyProfileClient
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
import org.springframework.transaction.PlatformTransactionManager

private const val ENRICH_CHUNK_SIZE = 20

// [배치] name이 ticker와 동일한(=토스 전용 유입이라 영문명을 못 채운, StockSeedJobConfig 참고) 종목을 대상으로
// Finnhub company profile에서 영문 정식 회사명을 조회해 name을 채운다.
// 청크 스텝(20건) - 조회 실패/미해결 건은 name=ticker로 그대로 남아 다음 배치 실행 때 재시도된다.
@Configuration
class StockEnglishNameEnrichJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val finnhubCompanyProfileClient: FinnhubCompanyProfileClient,
    private val stockRepository: StockRepository,
    @Value("\${stock.english-name-enrich.batch-size}") private val enrichBatchSize: Int,
) {
    private val log = LoggerFactory.getLogger(StockEnglishNameEnrichJobConfig::class.java)

    // Job: stockEnglishNameEnrichStep 단일 스텝으로 구성된 영문명 보강 배치 잡.
    @Bean
    fun stockEnglishNameEnrichJob(stockEnglishNameEnrichStep: Step): Job =
        JobBuilder("stockEnglishNameEnrichJob", jobRepository)
            .start(stockEnglishNameEnrichStep)
            .build()

    // Reader: name이 ticker와 동일한 종목을 batch-size만큼 조회해 한 건씩 꺼낸다.
    @Bean
    @StepScope
    fun stockEnglishNameEnrichReader(): ItemReader<Stock> {
        val candidates = stockRepository.findByNameEqualsTicker(enrichBatchSize).iterator()
        return ItemReader {
            if (candidates.hasNext()) {
                candidates.next()
            }
            else null
        }
    }

    // Processor: Finnhub에서 영문 정식 회사명을 조회해 채운다. 조회 실패/미해결이면 null을 반환해 이번 배치에서 제외한다(다음 실행 때 재시도).
    @Bean
    fun stockEnglishNameEnrichProcessor(): ItemProcessor<Stock, Stock> = ItemProcessor { stock ->
        val englishName = finnhubCompanyProfileClient.fetchEnglishName(stock.ticker)
        log.info("{} -> {}", stock.ticker, englishName ?: "unresolved")
        if (englishName != null) {
            stock.name = englishName
            stock
        } else {
            null
        }
    }

    // Writer: 영문명이 채워진 종목들을 그대로 저장한다.
    @Bean
    fun stockEnglishNameEnrichWriter(): ItemWriter<Stock> = ItemWriter { stocks ->
        stockRepository.saveAll(stocks.items)
    }

    @Bean
    fun stockEnglishNameEnrichStep(
        stockEnglishNameEnrichReader: ItemReader<Stock>,
        stockEnglishNameEnrichProcessor: ItemProcessor<Stock, Stock>,
        stockEnglishNameEnrichWriter: ItemWriter<Stock>,
    ): Step =
        StepBuilder("stockEnglishNameEnrichStep", jobRepository)
            .chunk<Stock, Stock>(ENRICH_CHUNK_SIZE)
            .reader(stockEnglishNameEnrichReader)
            .processor(stockEnglishNameEnrichProcessor)
            .writer(stockEnglishNameEnrichWriter)
            .transactionManager(transactionManager)
            .build()
}
