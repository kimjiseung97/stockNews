package org.kjs.stocknews.service

import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.UserStockResponse
import org.kjs.stocknews.model.table.UserStock
import org.kjs.stocknews.repository.StockRepository
import org.kjs.stocknews.repository.UserStockRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserStockService(
    private val userStockRepository: UserStockRepository,
    private val stockRepository: StockRepository,
) {
    @Transactional
    fun register(userId: Long, stockIds: List<Long>) {
        val distinctStockIds = stockIds.distinct()
        val stocks = stockRepository.findAllByIdIn(distinctStockIds)
        if (stocks.size != distinctStockIds.size) {
            throw BusinessException(ResultCode.STOCK_NOT_FOUND)
        }

        val alreadyRegisteredIds = mutableSetOf<Long>()
        for (userStock in userStockRepository.findAllByUserIdAndStockIdIn(userId, distinctStockIds)) {
            alreadyRegisteredIds.add(userStock.stockId)
        }

        val toRegister = mutableListOf<UserStock>()
        for (stockId in distinctStockIds) {
            if (stockId !in alreadyRegisteredIds) {
                toRegister.add(UserStock(userId = userId, stockId = stockId))
            }
        }
        userStockRepository.saveAll(toRegister)
    }

    fun list(userId: Long, pageable: Pageable): Page<UserStockResponse> =
        userStockRepository.search(userId, pageable)

    @Transactional
    fun unregister(userId: Long, stockIds: List<Long>) {
        val deleted = userStockRepository.deleteByUserIdAndStockIdIn(userId, stockIds.distinct())
        if (deleted == 0L) {
            throw BusinessException(ResultCode.USER_STOCK_NOT_FOUND)
        }
    }
}
