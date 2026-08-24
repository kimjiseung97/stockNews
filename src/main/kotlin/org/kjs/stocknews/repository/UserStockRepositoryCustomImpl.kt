package org.kjs.stocknews.repository

import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.ComparablePath
import com.querydsl.core.types.dsl.PathBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.kjs.stocknews.model.dto.UserStockNewsView
import org.kjs.stocknews.model.dto.UserStockResponse
import org.kjs.stocknews.model.table.QStock.stock
import org.kjs.stocknews.model.table.QUserStock.userStock
import org.kjs.stocknews.model.table.Stock
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository

@Repository
class UserStockRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : UserStockRepositoryCustom {
    private val pathBuilder = PathBuilder(Stock::class.java, stock.metadata)

    override fun search(userId: Long, pageable: Pageable): Page<UserStockResponse> {
        val orderSpecifiers = orderSpecifiers(pageable.sort).ifEmpty { listOf(userStock.id.asc()) }

        val content =
            queryFactory
                .select(
                    Projections.constructor(
                        UserStockResponse::class.java,
                        stock.id,
                        stock.ticker,
                        stock.name,
                        stock.theme,
                        stock.koreanName,
                    ),
                )
                .from(userStock)
                .join(stock).on(userStock.stockId.eq(stock.id))
                .where(userStock.userId.eq(userId))
                .orderBy(*orderSpecifiers.toTypedArray())
                .offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
                .fetch()

        val countQuery =
            queryFactory
                .select(userStock.count())
                .from(userStock)
                .where(userStock.userId.eq(userId))

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    // 청크에 포함된 유저 ID 전체를 한번에 join 조회해 유저별 개별 쿼리(N+1)를 없앤다.
    // 유저마다 종목이 달라도 상관없이 결과 행을 userId로 groupBy하면 각자 자기 종목만 골라 갖는다.
    override fun findNewsViewsByUserIdIn(userIds: List<Long>): List<UserStockNewsView> =
        queryFactory
            .select(
                Projections.constructor(
                    UserStockNewsView::class.java,
                    userStock.userId,
                    stock.id,
                    stock.ticker,
                    stock.name,
                    stock.koreanName,
                ),
            )
            .from(userStock)
            .join(stock).on(userStock.stockId.eq(stock.id))
            .where(userStock.userId.`in`(userIds))
            .fetch()

    override fun deleteByUserIdAndStockIdIn(userId: Long, stockIds: List<Long>): Long =
        queryFactory
            .delete(userStock)
            .where(userStock.userId.eq(userId), userStock.stockId.`in`(stockIds))
            .execute()

    private fun orderSpecifiers(sort: Sort): List<OrderSpecifier<*>> =
        sort.map { order ->
            val direction = if (order.isAscending) Order.ASC else Order.DESC
            @Suppress("UNCHECKED_CAST")
            val path = pathBuilder.getComparable(order.property, Comparable::class.java) as ComparablePath<Comparable<Any>>
            OrderSpecifier(direction, path)
        }.toList()
}
