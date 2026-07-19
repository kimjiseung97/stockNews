package org.kjs.stocknews.batch

import org.kjs.stocknews.model.table.Stock
import org.kjs.stocknews.repository.StockRepository
import org.kjs.stocknews.service.SecTickerClient
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.slf4j.LoggerFactory

@Configuration
class StockSeedJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val secTickerClient: SecTickerClient,
    private val stockRepository: StockRepository,
    @Value("\${stock.seed.batch-size}") private val seedBatchSize: Int,
) {
    private val log = LoggerFactory.getLogger(StockSeedJobConfig::class.java)

    @Bean
    fun stockSeedJob(stockSeedStep: Step): Job =
        JobBuilder("stockSeedJob", jobRepository)
            .start(stockSeedStep)
            .build()

    @Bean
    fun stockSeedStep(): Step =
        StepBuilder("stockSeedStep", jobRepository)
            .tasklet({ _, _ ->
                log.info("fetching SEC ticker list...")
                val existingTickers = stockRepository.findAllTickers().toHashSet()
                val candidates = secTickerClient.fetchAllTickers()
                    .filterNot { it.ticker in existingTickers }
                    .take(seedBatchSize)
                log.info("got {} new candidates to seed", candidates.size)

                val stocks = candidates.map { entry ->
                    Stock(ticker = entry.ticker, name = entry.title, cik = entry.cikStr)
                }
                stockRepository.saveAll(stocks)
                log.info("stock seed done: {} rows inserted (theme pending enrichment)", stocks.size)

                RepeatStatus.FINISHED
            }, transactionManager)
            .build()
}
