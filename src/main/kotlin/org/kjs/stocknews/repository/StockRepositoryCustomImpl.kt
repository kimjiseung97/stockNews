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

    // CIK가 있는 종목만 대상 - SEC 기업 프로필 조회(SecCompanyProfileClient)에 CIK가 필수라 없는 종목은 영구히 실패한다.
    override fun findByThemeIsNull(limit: Int): List<Stock> =
        queryFactory
            .selectFrom(stock)
            .where(stock.theme.isNull, stock.cik.isNotNull)
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

    // 한 번도 시도 안 한 종목(NULL) 우선, 그다음 가장 오래 전에 시도한(=가장 오래 실패해온) 종목 순으로 뽑는다.
    // 실패한 종목은 시도할 때마다 detailAttemptedAt이 now로 갱신되어 큐 맨 뒤로 밀려나므로,
    // id 오름차순 상위의 실패 종목이 뒤쪽 종목을 영구히 막는 문제가 생기지 않는다.
    override fun findWithoutDetail(limit: Int): List<Stock> =
        queryFactory
            .selectFrom(stock)
            .leftJoin(stockDetail).on(stockDetail.stockId.eq(stock.id))
            .where(stockDetail.id.isNull)
            .orderBy(stock.detailAttemptedAt.asc().nullsFirst())
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
