package org.kjs.stocknews.batch

import org.kjs.stocknews.model.table.Stock
import org.kjs.stocknews.repository.StockRepository
import org.kjs.stocknews.service.NaverStockNameClient
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

private const val ENRICH_CHUNK_SIZE = 20

// [배치] koreanName이 비어있는 종목을 대상으로 네이버에서 한글명을 조회해 채운다.
// 청크 스텝(20건) - 조회 실패/미해결 건은 null 반환으로 스킵하고 다음 배치 실행 때 재시도된다.
// limit 없이 대상 전체를 한 번에 읽는다(findByKoreanNameIsNull 주석 참고) - 영구 실패 종목이 뒤쪽 종목의
// 시도 기회를 막지 않도록 하기 위함. 잡이 오래 걸릴 수 있어 스케줄러가 중복 실행은 스킵한다.
@Configuration
class StockKoreanNameEnrichJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val naverStockNameClient: NaverStockNameClient,
    private val stockRepository: StockRepository,
) {
    private val log = LoggerFactory.getLogger(StockKoreanNameEnrichJobConfig::class.java)

    // Job: stockKoreanNameEnrichStep 단일 스텝으로 구성된 한글명 보강 배치 잡.
    @Bean
    fun stockKoreanNameEnrichJob(stockKoreanNameEnrichStep: Step): Job =
        JobBuilder("stockKoreanNameEnrichJob", jobRepository)
            .start(stockKoreanNameEnrichStep)
            .build()

    // Reader: koreanName이 비어있는 종목 전체를 조회해 한 건씩 꺼낸다.
    @Bean
    @StepScope
    fun stockKoreanNameEnrichReader(): ItemReader<Stock> {
        val candidates = stockRepository.findByKoreanNameIsNull().iterator()
        return ItemReader {
            if (candidates.hasNext()) {
                candidates.next()
            }
            else null
        }
    }

    // Processor: 네이버에서 종목의 한글명을 조회해 채운다. 조회 실패/미해결이면 null을 반환해 이번 배치에서 제외한다(다음 실행 때 재시도).
    @Bean
    fun stockKoreanNameEnrichProcessor(): ItemProcessor<Stock, Stock> = ItemProcessor { stock ->
        try {
            val koreanName = naverStockNameClient.fetchKoreanName(stock.ticker)
            log.info("{} -> {}", stock.ticker, koreanName ?: "unresolved")
            if (koreanName != null) {
                stock.koreanName = koreanName
                stock
            } else {
                null
            }

        } catch (e: Exception) {
            log.warn("{} -> failed: {}", stock.ticker, e.message)
            null
        }
    }

    // Writer: 한글명이 채워진 종목들을 그대로 저장한다.
    @Bean
    fun stockKoreanNameEnrichWriter(): ItemWriter<Stock> = ItemWriter { stocks ->
        stockRepository.saveAll(stocks.items)
    }

    @Bean
    fun stockKoreanNameEnrichStep(
        stockKoreanNameEnrichReader: ItemReader<Stock>,
        stockKoreanNameEnrichProcessor: ItemProcessor<Stock, Stock>,
        stockKoreanNameEnrichWriter: ItemWriter<Stock>,
    ): Step =
        StepBuilder("stockKoreanNameEnrichStep", jobRepository)
            .chunk<Stock, Stock>(ENRICH_CHUNK_SIZE)
            .reader(stockKoreanNameEnrichReader)
            .processor(stockKoreanNameEnrichProcessor)
            .writer(stockKoreanNameEnrichWriter)
            .transactionManager(transactionManager)
            .build()
}
