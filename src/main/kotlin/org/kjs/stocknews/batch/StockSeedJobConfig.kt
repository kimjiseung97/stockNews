package org.kjs.stocknews.batch

import org.kjs.stocknews.model.table.Stock
import org.kjs.stocknews.repository.StockRepository
import org.kjs.stocknews.service.NasdaqListedClient
import org.kjs.stocknews.service.SecTickerClient
import org.kjs.stocknews.service.TossStockClient
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

// [배치] SEC 티커 목록 + 나스닥/그 외 거래소 상장 목록(NasdaqListedClient) + 토스증권 미국주식 목록(TossStockClient)에서
// TB_STOCK에 없는 신규 종목을 찾아 기본 정보(ticker/name/cik)만 배치 삽입한다.
// SEC 출신은 CIK를 채우고, 거래소 목록/토스 전용 종목은 CIK가 없어 null로 저장.
// 토스는 name이 한글로만 와서(예: AAPL -> "애플") koreanName에 매핑하고, 영문 name은 우선 티커 심볼로 채운다
// (이후 stockKoreanNameEnrichJob/향후 영문명 보강이 필요하면 별도 보강 대상).
// 단일 tasklet - 테마(theme)는 채우지 않고 이후 stockThemeEnrichJob이 보강(CIK 있는 종목만 대상).
@Configuration
class StockSeedJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val secTickerClient: SecTickerClient,
    private val nasdaqListedClient: NasdaqListedClient,
    private val tossStockClient: TossStockClient,
    private val stockRepository: StockRepository,
    @Value("\${stock.seed.batch-size}") private val seedBatchSize: Int,
) {
    private val log = LoggerFactory.getLogger(StockSeedJobConfig::class.java)

    // Job: stockSeedStep 단일 스텝으로 구성된 종목 시딩 배치 잡.
    @Bean
    fun stockSeedJob(stockSeedStep: Step): Job =
        JobBuilder("stockSeedJob", jobRepository)
            .start(stockSeedStep)
            .build()

    // Tasklet(Reader+Processor+Writer 역할 겸용): SEC + 나스닥/그 외 거래소 + 토스증권 목록을 조회해 DB에 없는
    // 신규 종목만 골라 batch-size만큼 잘라 기본 정보로 저장한다.
    // 우선순위: SEC 후보 -> 거래소 전용 후보 -> 토스 전용 후보(둘 다에 없는 진짜 갭 채우기) 순으로 채운다.
    @Bean
    fun stockSeedStep(): Step =
        StepBuilder("stockSeedStep", jobRepository)
            .tasklet({ _, _ ->
                log.info("fetching sec + nasdaq/other exchange + toss ticker lists...")
                val existingTickers = stockRepository.findAllTickers().toHashSet()

                val secCandidates = secTickerClient.fetchAllTickers()
                    .filterNot { it.ticker in existingTickers }
                    .map { Stock(ticker = it.ticker, name = it.title, cik = it.cikStr) }

                val seenAfterSec = existingTickers.plus(secCandidates.map { it.ticker })
                val exchangeCandidates = nasdaqListedClient.fetchAllListed()
                    .distinctBy { it.ticker }
                    .filterNot { it.ticker in seenAfterSec }
                    .map { Stock(ticker = it.ticker, name = it.name, cik = null) }

                val seenAfterExchange = seenAfterSec.plus(exchangeCandidates.map { it.ticker })
                val tossCandidates = tossStockClient.fetchAllUsListed()
                    .filterNot { it.symbol in seenAfterExchange }
                    .map { Stock(ticker = it.symbol, name = it.symbol, cik = null).apply { koreanName = it.name } }

                val stocks = secCandidates.plus(exchangeCandidates).plus(tossCandidates).take(seedBatchSize)
                log.info(
                    "got {} new candidates to seed ({} sec, {} exchange-only, {} toss-only)",
                    stocks.size,
                    secCandidates.size,
                    exchangeCandidates.size,
                    tossCandidates.size,
                )

                stockRepository.saveAll(stocks)
                log.info("stock seed done: {} rows inserted (theme pending enrichment)", stocks.size)

                RepeatStatus.FINISHED
            }, transactionManager)
            .build()
}
