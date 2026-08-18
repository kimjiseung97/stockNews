package org.kjs.stocknews.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.PopularStockResponse
import org.kjs.stocknews.model.table.StockDetail
import org.kjs.stocknews.repository.StockDetailRepository
import org.kjs.stocknews.repository.StockRepository
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class StockServiceTest {
    private val stockRepository = mock(StockRepository::class.java)
    private val stockDetailRepository = mock(StockDetailRepository::class.java)
    private val stockSearchCountService = mock(StockSearchCountService::class.java)
    private val stockService = StockService(stockRepository, stockDetailRepository, stockSearchCountService)

    @Test
    fun `상세정보가 존재하면 조회 시 응답으로 매핑된다`() {
        val stockDetail = StockDetail(
            stockId = 1L,
            summary = "요약",
            representativeName = "홍길동",
            nation = "United States",
            city = "CUPERTINO, CA",
            homepageUrl = "https://example.com",
            industryName = "반도체",
        )
        `when`(stockDetailRepository.findByStockId(1L)).thenReturn(stockDetail)

        val response = stockService.getDetail(1L)

        assert(response.stockId == 1L)
        assert(response.representativeName == "홍길동")
        assert(response.industryName == "반도체")
        verify(stockSearchCountService).saveSearchCount(1L)
    }

    @Test
    fun `상세정보가 없으면 조회 시 STOCK_DETAIL_NOT_FOUND 예외가 발생한다`() {
        `when`(stockDetailRepository.findByStockId(1L)).thenReturn(null)

        val exception = assertThrows<BusinessException> { stockService.getDetail(1L) }
        assert(exception.resultCode == ResultCode.STOCK_DETAIL_NOT_FOUND)
    }

    @Test
    fun `인기종목 조회는 repository 결과를 그대로 반환한다`() {
        val popularStocks = listOf(
            PopularStockResponse(id = 1L, ticker = "NVDA", name = "NVIDIA CORP", theme = null, koreanName = "엔비디아", searchCount = 3L),
        )
        `when`(stockRepository.findPopularStocks(5)).thenReturn(popularStocks)

        val response = stockService.getPopularStocks(5)

        assert(response == popularStocks)
    }
}
