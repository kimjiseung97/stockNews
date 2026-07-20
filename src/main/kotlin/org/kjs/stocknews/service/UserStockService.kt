package org.kjs.stocknews.service

import org.kjs.stocknews.common.CustomException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.UserStockResponse
import org.kjs.stocknews.model.table.UserStock
import org.kjs.stocknews.repository.StockRepository
import org.kjs.stocknews.repository.UserStockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserStockService(
    private val userStockRepository: UserStockRepository,
    private val stockRepository: StockRepository,
) {
    @Transactional
    fun register(userId: Long, tickers: List<String>) {
        val distinctTickers = tickers.distinct()
        val stocks = stockRepository.findAllByTickerIn(distinctTickers)
        if (stocks.size != distinctTickers.size) {
            throw CustomException(ResultCode.STOCK_NOT_FOUND)
        }

        val stockIds = mutableListOf<Long>()
        for (stock in stocks) {
            stockIds.add(stock.id!!)
        }

        val alreadyRegisteredIds = mutableSetOf<Long>()
        for (userStock in userStockRepository.findAllByUserIdAndStockIdIn(userId, stockIds)) {
            alreadyRegisteredIds.add(userStock.stockId)
        }

        val toRegister = mutableListOf<UserStock>()
        for (stockId in stockIds) {
            if (stockId !in alreadyRegisteredIds) {
                toRegister.add(UserStock(userId = userId, stockId = stockId))
            }
        }
        userStockRepository.saveAll(toRegister)
    }

    fun list(userId: Long): List<UserStockResponse> {
        val userStocks = userStockRepository.findAllByUserId(userId)

        val stockIds = mutableListOf<Long>()
        for (userStock in userStocks) {
            stockIds.add(userStock.stockId)
        }
        val stocksById = stockRepository.findAllById(stockIds).associateBy { it.id }

        val result = mutableListOf<UserStockResponse>()
        for (userStock in userStocks) {
            val stock = stocksById[userStock.stockId] ?: continue
            result.add(UserStockResponse(ticker = stock.ticker, name = stock.name, theme = stock.theme))
        }
        return result
    }

    @Transactional
    fun unregister(userId: Long, ticker: String) {
        val stock = stockRepository.findByTicker(ticker) ?: throw CustomException(ResultCode.STOCK_NOT_FOUND)
        val deleted = userStockRepository.deleteByUserIdAndStockId(userId, stock.id!!)
        if (deleted == 0L) {
            throw CustomException(ResultCode.USER_STOCK_NOT_FOUND)
        }
    }
}
