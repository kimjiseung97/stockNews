package org.kjs.stocknews.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.kjs.stocknews.model.table.StockSearchCount
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

// findWithoutDetail이 detailAttemptedAt 오름차순(NULL 우선)으로 조회되는지 확인한다.
@SpringBootTest
class StockRepositoryCustomImplTest(
    @Autowired private val stockRepository: StockRepository,
    @Autowired private val stockSearchCountRepository: StockSearchCountRepository,
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
}
