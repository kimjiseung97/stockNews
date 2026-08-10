package org.kjs.stocknews.repository

import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.ComparablePath
import com.querydsl.core.types.dsl.PathBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.kjs.stocknews.model.table.QStock.stock
import org.kjs.stocknews.model.table.QStockDetail.stockDetail
import org.kjs.stocknews.model.table.Stock
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository

@Repository
class StockRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : StockRepositoryCustom {
    private val pathBuilder = PathBuilder(Stock::class.java, stock.metadata)

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

    override fun findByKoreanNameIsNull(limit: Int): List<Stock> =
        queryFactory
            .selectFrom(stock)
            .where(stock.koreanName.isNull)
            .limit(limit.toLong())
            .fetch()

    override fun findWithoutDetail(limit: Int): List<Stock> =
        queryFactory
            .selectFrom(stock)
            .leftJoin(stockDetail).on(stockDetail.stockId.eq(stock.id))
            .where(stockDetail.id.isNull)
            .limit(limit.toLong())
            .fetch()

    override fun search(keyword: String?, pageable: Pageable): Page<Stock> {
        val orderSpecifiers = orderSpecifiers(pageable.sort).ifEmpty { listOf(stock.id.asc()) }

        val content =
            queryFactory
                .selectFrom(stock)
                .where(keywordContains(keyword))
                .orderBy(*orderSpecifiers.toTypedArray())
                .offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
                .fetch()

        val countQuery =
            queryFactory
                .select(stock.count())
                .from(stock)
                .where(keywordContains(keyword))

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    private fun keywordContains(keyword: String?): BooleanExpression? {
        val trimmed = keyword?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return stock.koreanName.contains(trimmed)
    }

    private fun orderSpecifiers(sort: Sort): List<OrderSpecifier<*>> =
        sort.map { order ->
            val direction = if (order.isAscending) {
                Order.ASC
            } else {
                Order.DESC
            }
            @Suppress("UNCHECKED_CAST")
            val path = pathBuilder.getComparable(order.property, Comparable::class.java) as ComparablePath<Comparable<Any>>
            OrderSpecifier(direction, path)
        }.toList()
}
