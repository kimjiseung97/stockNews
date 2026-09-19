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

                // 소스 하나가 죽어도 나머지로 이어서 시딩할 수 있게, 조회 실패는 빈 목록으로 흘린다.
                val secEntries = try {
                    secTickerClient.fetchAllTickers()
                } catch (e: Exception) {
                    log.warn("sec ticker fetch failed, skipping this source for this run", e)
                    emptyList()
                }

                val newSecStockList = mutableListOf<Stock>()
                for (entry in secEntries) {
                    if (entry.ticker in existingTickers) {
                        continue
                    }
                    newSecStockList.add(Stock(ticker = entry.ticker, name = entry.title, cik = entry.cikStr))
                }

                // 이미 DB에 있거나 SEC에서 담은 티커. 뒤 소스에서 같은 티커를 또 담지 않기 위한 누적 집합이다.
                val seenTickers = HashSet(existingTickers)
                for (stock in newSecStockList) {
                    seenTickers.add(stock.ticker)
                }

                val exchangeEntries = try {
                    nasdaqListedClient.fetchAllListed()
                } catch (e: Exception) {
                    log.warn("nasdaq listed fetch failed, skipping this source for this run", e)
                    emptyList()
                }

                val newExchangeStockList = mutableListOf<Stock>()
                for (entry in exchangeEntries) {
                    // seenTickers에 넣어가며 거르므로 거래소 목록 안의 중복 티커도 같이 걸러진다.
                    if (entry.ticker in seenTickers) {
                        continue
                    }
                    seenTickers.add(entry.ticker)
                    newExchangeStockList.add(Stock(ticker = entry.ticker, name = entry.name, cik = null))
                }

                val tossEntries = try {
                    tossStockClient.fetchAllUsListed()
                } catch (e: Exception) {
                    log.warn("toss listed fetch failed, skipping this source for this run", e)
                    emptyList()
                }

                val newTossStockList = mutableListOf<Stock>()
                for (entry in tossEntries) {
                    // 거래소 목록과 마찬가지로 seenTickers에 넣어가며 거른다. TB_STOCK.TICKER가 unique라
                    // 토스 목록 안에 같은 심볼이 두 번 오면 저장 단계에서 제약 위반이 나기 때문이다.
                    if (entry.symbol in seenTickers) {
                        continue
                    }
                    seenTickers.add(entry.symbol)
                    // 토스는 한글명만 주므로 name에는 심볼을 넣고 한글명을 따로 채운다.
                    val stock = Stock(ticker = entry.symbol, name = entry.symbol, cik = null)
                    stock.koreanName = entry.name
                    newTossStockList.add(stock)
                }

                val newStockList = mutableListOf<Stock>()
                newStockList.addAll(newSecStockList)
                newStockList.addAll(newExchangeStockList)
                newStockList.addAll(newTossStockList)

                val stocks = newStockList.take(seedBatchSize)
                log.info(
                    "got {} new stocks to seed ({} sec, {} exchange-only, {} toss-only)",
                    stocks.size,
                    newSecStockList.size,
                    newExchangeStockList.size,
                    newTossStockList.size,
                )

                stockRepository.saveAll(stocks)
                log.info("stock seed done: {} rows inserted (theme pending enrichment)", stocks.size)

                RepeatStatus.FINISHED
            }, transactionManager)
            .build()
}
