package org.kjs.stocknews.batch

import org.kjs.stocknews.model.table.Stock
import org.kjs.stocknews.model.table.StockDetail
import org.kjs.stocknews.repository.StockDetailRepository
import org.kjs.stocknews.repository.StockRepository
import org.kjs.stocknews.service.NaverStockDetailClient
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
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDateTime
import java.time.OffsetDateTime

private const val ENRICH_CHUNK_SIZE = 20

// [배치] TB_STOCK_DETAIL이 없는 종목을 대상으로 네이버 기업개요 API에서 상세정보를 조회해 채운다.
// 청크 스텝(20건) - reutersCode 조회/기업개요 조회 실패 건은 null 반환으로 스킵하고 다음 배치 실행 때 재시도된다.
@Configuration
class StockDetailEnrichJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val naverStockNameClient: NaverStockNameClient,
    private val naverStockDetailClient: NaverStockDetailClient,
    private val stockRepository: StockRepository,
    private val stockDetailRepository: StockDetailRepository,
    @Value("\${stock.detail-enrich.batch-size}") private val enrichBatchSize: Int,
) {
    private val log = LoggerFactory.getLogger(StockDetailEnrichJobConfig::class.java)

    // Job: stockDetailEnrichStep 단일 스텝으로 구성된 상세정보 보강 배치 잡.
    @Bean
    fun stockDetailEnrichJob(stockDetailEnrichStep: Step): Job =
        JobBuilder("stockDetailEnrichJob", jobRepository)
            .start(stockDetailEnrichStep)
            .build()

    // Reader: TB_STOCK_DETAIL이 없는 종목을 batch-size만큼 조회해 한 건씩 꺼낸다.
    @Bean
    @StepScope
    fun stockDetailEnrichReader(): ItemReader<Stock> {
        val candidates = stockRepository.findWithoutDetail(enrichBatchSize).iterator()
        return ItemReader {
            if (candidates.hasNext()) {
                candidates.next()
            }
            else null
        }
    }

    // Processor: 네이버에서 reutersCode를 조회한 뒤 기업개요 API로 상세정보를 채운다.
    // reutersCode 미해결/기업개요 조회 실패면 null을 반환해 이번 배치에서 제외한다(다음 실행 때 재시도).
    // 성공/실패 무관하게 stock.detailAttemptedAt을 갱신해, OTC/상장폐지 등으로 영구 실패하는 종목이
    // findWithoutDetail의 재시도 우선순위 맨 앞을 계속 차지하지 않도록 한다.
    @Bean
    fun stockDetailEnrichProcessor(): ItemProcessor<Stock, StockDetail> = ItemProcessor { stock ->
        try {
            val reutersCode = naverStockNameClient.fetchReutersCode(stock.ticker)
            if (reutersCode == null) {
                log.info("{} -> reutersCode unresolved", stock.ticker)
                return@ItemProcessor null
            }

            val overview = naverStockDetailClient.fetchOverview(reutersCode)
            if (overview == null) {
                log.info("{} -> overview not found", stock.ticker)
                return@ItemProcessor null
            }

            log.info("{} -> {}", stock.ticker, reutersCode)
            StockDetail(
                stockId = stock.id!!,
                summary = overview.summary,
                representativeName = overview.summaries?.representativeName,
                nation = overview.summaries?.nation,
                city = overview.summaries?.city,
                homepageUrl = overview.summaries?.url,
                industryName = overview.industry?.industryGroupKor,
                listedAt = overview.stockItemListedInfo?.listedAt?.let { parseListedAt(it) },
            )
        } catch (e: Exception) {
            log.warn("{} -> failed: {}", stock.ticker, e.message)
            null
        } finally {
            stock.detailAttemptedAt = LocalDateTime.now()
            stockRepository.save(stock)
        }
    }

    private fun parseListedAt(listedAt: String) =
        try {
            OffsetDateTime.parse(listedAt).toLocalDateTime()
        } catch (e: Exception) {
            null
        }

    // Writer: 조회에 성공한 상세정보를 그대로 저장한다.
    @Bean
    fun stockDetailEnrichWriter(): ItemWriter<StockDetail> = ItemWriter { details ->
        stockDetailRepository.saveAll(details.items)
    }

    @Bean
    fun stockDetailEnrichStep(
        stockDetailEnrichReader: ItemReader<Stock>,
        stockDetailEnrichProcessor: ItemProcessor<Stock, StockDetail>,
        stockDetailEnrichWriter: ItemWriter<StockDetail>,
    ): Step =
        StepBuilder("stockDetailEnrichStep", jobRepository)
            .chunk<Stock, StockDetail>(ENRICH_CHUNK_SIZE)
            .reader(stockDetailEnrichReader)
            .processor(stockDetailEnrichProcessor)
            .writer(stockDetailEnrichWriter)
            .transactionManager(transactionManager)
            .build()
}
