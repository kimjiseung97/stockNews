package org.kjs.stocknews.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.PopularStockResponse
import org.kjs.stocknews.model.table.StockDetail
import org.kjs.stocknews.model.table.StockNews
import org.kjs.stocknews.repository.StockDetailRepository
import org.kjs.stocknews.repository.StockNewsRepository
import org.kjs.stocknews.repository.StockRepository
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class StockServiceTest {
    private val stockRepository = mock(StockRepository::class.java)
    private val stockDetailRepository = mock(StockDetailRepository::class.java)
    private val stockSearchCountService = mock(StockSearchCountService::class.java)
    private val stockNewsRepository = mock(StockNewsRepository::class.java)
    private val stockService =
        StockService(stockRepository, stockDetailRepository, stockSearchCountService, stockNewsRepository)

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

    @Test
    fun `존재하는 종목의 뉴스 조회는 최신순 페이징 결과를 응답으로 매핑한다`() {
        val pageable = PageRequest.of(0, 10)
        val stockNews = StockNews(
            stockId = 1L,
            title = "제목",
            content = "내용",
            url = "https://news.example.com/1",
        ).setTestId(1L)
        `when`(stockRepository.existsById(1L)).thenReturn(true)
        `when`(stockNewsRepository.findByStockIdOrderByCollectedAtDesc(1L, pageable))
            .thenReturn(PageImpl(listOf(stockNews)))

        val response = stockService.getNews(1L, pageable)

        assert(response.content.size == 1)
        assert(response.content[0].title == "제목")
        assert(response.content[0].url == "https://news.example.com/1")
    }

    @Test
    fun `존재하지 않는 종목의 뉴스 조회는 STOCK_NOT_FOUND 예외가 발생한다`() {
        val pageable = PageRequest.of(0, 10)
        `when`(stockRepository.existsById(1L)).thenReturn(false)

        val exception = assertThrows<BusinessException> { stockService.getNews(1L, pageable) }
        assert(exception.resultCode == ResultCode.STOCK_NOT_FOUND)
    }

    private fun StockNews.setTestId(id: Long): StockNews {
        val field = StockNews::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(this, id)
        return this
    }
}
