package org.kjs.stocknews.service

import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.PopularStockResponse
import org.kjs.stocknews.model.dto.StockDetailResponse
import org.kjs.stocknews.model.dto.StockNewsResponse
import org.kjs.stocknews.model.dto.StockResponse
import org.kjs.stocknews.repository.StockDetailRepository
import org.kjs.stocknews.repository.StockNewsRepository
import org.kjs.stocknews.repository.StockRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class StockService(
    private val stockRepository: StockRepository,
    private val stockDetailRepository: StockDetailRepository,
    private val stockSearchCountService: StockSearchCountService,
    private val stockNewsRepository: StockNewsRepository,
) {
    fun search(keyword: String?, pageable: Pageable): Page<StockResponse> =
        stockRepository.search(keyword, pageable).map { StockResponse.from(it) }

    fun getDetail(stockId: Long): StockDetailResponse {
        val stockDetail = stockDetailRepository.findByStockId(stockId)
            ?: throw BusinessException(ResultCode.STOCK_DETAIL_NOT_FOUND)
        stockSearchCountService.saveSearchCount(stockId)
        return StockDetailResponse.from(stockDetail)
    }

    fun getPopularStocks(limit: Int): List<PopularStockResponse> = stockRepository.findPopularStocks(limit)

    fun getNews(stockId: Long, pageable: Pageable): Page<StockNewsResponse> {
        if (!stockRepository.existsById(stockId)) {
            throw BusinessException(ResultCode.STOCK_NOT_FOUND)
        }
        return stockNewsRepository.findByStockIdOrderByCollectedAtDesc(stockId, pageable).map { StockNewsResponse.from(it) }
    }
}
