package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.UserStock
import org.springframework.data.jpa.repository.JpaRepository

interface UserStockRepository : JpaRepository<UserStock, Long> {
    fun existsByUserIdAndStockId(userId: Long, stockId: Long): Boolean
    fun findAllByUserId(userId: Long): List<UserStock>
    fun findAllByUserIdAndStockIdIn(userId: Long, stockIds: List<Long>): List<UserStock>
    fun deleteByUserIdAndStockId(userId: Long, stockId: Long): Long
}
