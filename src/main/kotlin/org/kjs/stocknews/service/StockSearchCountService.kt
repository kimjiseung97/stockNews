package org.kjs.stocknews.service

import org.kjs.stocknews.model.table.StockSearchCount
import org.kjs.stocknews.repository.StockSearchCountRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

// 검색/상세조회/관심종목 등록 통계 로깅 전담 서비스.
// REQUIRES_NEW로 별도 트랜잭션에서 커밋하므로, 로그 적재 실패가 호출한 쪽의 본 트랜잭션(상세조회, 관심종목 등록 등)을 롤백시키지 않는다.
@Service
class StockSearchCountService(
    private val stockSearchCountRepository: StockSearchCountRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveSearchCount(stockId: Long) {
        try {
            stockSearchCountRepository.save(StockSearchCount(stockId = stockId))
        } catch (e: Exception) {
            log.warn("종목 검색 통계 적재 실패 - stockId={}", stockId, e)
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveSearchCountAll(stockIds: List<Long>) {
        if (stockIds.isEmpty()) return
        try {
            stockSearchCountRepository.saveAll(stockIds.map { StockSearchCount(stockId = it) })
        } catch (e: Exception) {
            log.warn("종목 검색 통계 적재 실패 - stockIds={}", stockIds, e)
        }
    }
}
