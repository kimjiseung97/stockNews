package org.kjs.stocknews.repository

import org.kjs.stocknews.model.dto.UserStockResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserStockRepositoryCustom {
    fun search(userId: Long, pageable: Pageable): Page<UserStockResponse>
    fun deleteByUserIdAndStockIdIn(userId: Long, stockIds: List<Long>): Long
}
