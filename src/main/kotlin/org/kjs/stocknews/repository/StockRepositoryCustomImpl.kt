package org.kjs.stocknews.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import org.kjs.stocknews.model.table.QStock.stock
import org.kjs.stocknews.model.table.Stock
import org.springframework.stereotype.Repository

@Repository
class StockRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : StockRepositoryCustom {
    override fun findByThemeIsNull(limit: Int): List<Stock> =
        queryFactory
            .selectFrom(stock)
            .where(stock.theme.isNull)
            .limit(limit.toLong())
            .fetch()

    override fun findAllByIdIn(ids: List<Long>): List<Stock> =
        queryFactory
            .selectFrom(stock)
            .where(stock.id.`in`(ids))
            .fetch()
}
