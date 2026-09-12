package org.kjs.stocknews.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.kjs.stocknews.model.table.StockSearchCount
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

// findWithoutDetail이 detailAttemptedAt 오름차순(NULL 우선)으로 조회되는지 확인한다.
@SpringBootTest
class StockRepositoryCustomImplTest(
    @Autowired private val stockRepository: StockRepository,
    @Autowired private val stockSearchCountRepository: StockSearchCountRepository,
    @Autowired private val stockDetailRepository: StockDetailRepository,
) {

    @AfterEach
    fun cleanUp() {
        stockSearchCountRepository.deleteAll(insertedSearchCounts)
        insertedSearchCounts.clear()
    }

    private val insertedSearchCounts = mutableListOf<StockSearchCount>()

    @Test
    fun `findWithoutDetail은 detailAttemptedAt이 이른 순으로 조회된다`() {
        val candidates = stockRepository.findWithoutDetail(20)
        assertThat(candidates).isNotEmpty

        val attemptedAts = candidates.map { it.detailAttemptedAt ?: LocalDateTime.MIN }
        assertThat(attemptedAts).isSorted
    }

    @Test
    fun `findPopularStocks는 금일 검색건수 내림차순으로 조회된다`() {
        val stocks = stockRepository.findAll().take(2)
        assertThat(stocks).hasSize(2)
        val (moreSearchedStock, lessSearchedStock) = stocks

        insertedSearchCounts += stockSearchCountRepository.saveAll(
            listOf(
                StockSearchCount(stockId = moreSearchedStock.id!!),
                StockSearchCount(stockId = moreSearchedStock.id!!),
                StockSearchCount(stockId = lessSearchedStock.id!!),
            ),
        )

        val popularStocks = stockRepository.findPopularStocks(1000)
        val rankOf = { stockId: Long -> popularStocks.indexOfFirst { it.id == stockId } }

        assertThat(rankOf(moreSearchedStock.id!!)).isGreaterThanOrEqualTo(0)
        assertThat(rankOf(lessSearchedStock.id!!)).isGreaterThanOrEqualTo(0)
        assertThat(rankOf(moreSearchedStock.id!!)).isLessThan(rankOf(lessSearchedStock.id!!))
    }

    @Test
    fun `findPopularStocks는 limit 개수를 넘지 않는다`() {
        assertThat(stockRepository.findPopularStocks(1)).hasSizeLessThanOrEqualTo(1)
    }

    // 목록 응답의 theme은 SIC 기반 enum이 아니라 TB_STOCK_DETAIL의 한글 산업명이어야 한다.
    @Test
    fun `search는 상세정보의 한글 산업명을 theme으로 내려준다`() {
        val detail = stockDetailRepository.findAll().first { it.industryName != null }
        val stock = stockRepository.findById(detail.stockId).orElseThrow()

        val found = stockRepository.search(stock.ticker, PageRequest.of(0, 50))
            .content
            .first { it.id == stock.id }

        assertThat(found.theme).isEqualTo(detail.industryName)
    }

    // 상세정보가 아직 수집되지 않은 종목도 leftJoin이라 목록에서 빠지지 않고 theme만 null로 내려간다.
    @Test
    fun `search는 상세정보가 없는 종목도 theme만 null로 두고 함께 조회한다`() {
        val stock = stockRepository.findWithoutDetail(1).first()

        val found = stockRepository.search(stock.ticker, PageRequest.of(0, 50))
            .content
            .firstOrNull { it.id == stock.id }

        assertThat(found).isNotNull
        assertThat(found!!.theme).isNull()
    }

    @Test
    fun `findPopularStocks도 상세정보의 한글 산업명을 theme으로 내려준다`() {
        val detail = stockDetailRepository.findAll().first { it.industryName != null }

        insertedSearchCounts += stockSearchCountRepository.save(StockSearchCount(stockId = detail.stockId))

        val popular = stockRepository.findPopularStocks(1000).first { it.id == detail.stockId }

        assertThat(popular.theme).isEqualTo(detail.industryName)
    }
}
