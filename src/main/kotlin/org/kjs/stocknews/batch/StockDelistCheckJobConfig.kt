package org.kjs.stocknews.batch

import org.kjs.stocknews.model.table.StockStatus
import org.kjs.stocknews.repository.StockRepository
import org.kjs.stocknews.service.NasdaqListedClient
import org.kjs.stocknews.service.SecTickerClient
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

// 두 소스 모두 일시적으로 비정상(네트워크 오류로 텅 비거나 극히 일부만 응답)일 때
// ACTIVE 종목 전체가 상장폐지로 오판되는 걸 막기 위한 최소 기준치.
private const val MIN_EXPECTED_SEC_TICKER_COUNT = 5000
private const val MIN_EXPECTED_EXCHANGE_TICKER_COUNT = 5000

// [배치] SEC 티커 목록과 나스닥/그 외 거래소 상장 목록(NasdaqListedClient) "둘 다"에 더 이상 존재하지 않는
// ACTIVE 종목만 상장폐지(DELISTED)로 표시한다. 물리 삭제 금지 - TB_USER_STOCK이 STOCK_ID를 논리 참조하므로 상태 컬럼만 갱신.
// SEC company_tickers.json은 "SEC 보고 의무 등록 스냅샷"이라 실제로는 멀쩡히 거래 중인 종목이 일시적으로
// 누락되는 경우가 있다(실측: AEP, GTLS 등 대형 상장사도 특정 시점에 빠져있었음) - 단일 소스만으로 판단하면
// 대량 오탐이 발생하므로 반드시 두 소스 모두에서 부재를 확인한다.
// 단일 tasklet - 두 소스 중 하나라도 응답이 비정상적으로 작으면(MIN_EXPECTED_*_TICKER_COUNT 미만) 이번 실행은 스킵한다.
@Configuration
class StockDelistCheckJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val secTickerClient: SecTickerClient,
    private val nasdaqListedClient: NasdaqListedClient,
    private val stockRepository: StockRepository,
) {
    private val log = LoggerFactory.getLogger(StockDelistCheckJobConfig::class.java)

    // Job: stockDelistCheckStep 단일 스텝으로 구성된 상장폐지 판별 배치 잡.
    @Bean
    fun stockDelistCheckJob(stockDelistCheckStep: Step): Job =
        JobBuilder("stockDelistCheckJob", jobRepository)
            .start(stockDelistCheckStep)
            .build()

    // Tasklet: SEC + 나스닥/그 외 거래소 목록을 모두 조회해, 현재 ACTIVE 종목 중 "두 목록 모두"에 없는
    // 종목만 DELISTED로 표시한다(물리 삭제 없음). 한쪽 소스에만 없는 경우는 그 소스의 일시적 누락일 수 있어 보류.
    @Bean
    fun stockDelistCheckStep(): Step =
        StepBuilder("stockDelistCheckStep", jobRepository)
            .tasklet({ _, _ ->
                val secTickers = secTickerClient.fetchAllTickers().map { it.ticker }.toHashSet()
                val exchangeTickers = nasdaqListedClient.fetchAllListed().map { it.ticker }.toHashSet()

                if (secTickers.size < MIN_EXPECTED_SEC_TICKER_COUNT || exchangeTickers.size < MIN_EXPECTED_EXCHANGE_TICKER_COUNT) {
                    log.warn(
                        "ticker source(s) look abnormal (sec={}, exchange={}), skipping delist check",
                        secTickers.size,
                        exchangeTickers.size,
                    )
                    return@tasklet RepeatStatus.FINISHED
                }

                val activeStocks = stockRepository.findByStatus(StockStatus.ACTIVE)
                val toDelist = activeStocks.filterNot { it.ticker in secTickers || it.ticker in exchangeTickers }
                log.info("checked {} active stocks, {} in neither sec nor exchange ticker list", activeStocks.size, toDelist.size)

                if (toDelist.isNotEmpty()) {
                    val now = LocalDateTime.now()
                    toDelist.forEach { stock ->
                        stock.status = StockStatus.DELISTED
                        stock.delistedAt = now
                    }
                    stockRepository.saveAll(toDelist)
                    log.info("marked delisted: {}", toDelist.joinToString(", ") { it.ticker })
                }

                RepeatStatus.FINISHED
            }, transactionManager)
            .build()
}
