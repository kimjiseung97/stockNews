package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.StockResponse
import org.kjs.stocknews.repository.StockRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class StockService(
    private val stockRepository: StockRepository,
) {
    fun search(keyword: String?, pageable: Pageable): Page<StockResponse> =
        stockRepository.search(keyword, pageable).map { StockResponse.from(it) }
}
