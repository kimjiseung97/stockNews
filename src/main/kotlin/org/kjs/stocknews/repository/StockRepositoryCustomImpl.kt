package org.kjs.stocknews.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.kjs.stocknews.model.table.QStock.stock
import org.kjs.stocknews.model.table.Stock
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
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

    override fun findByKoreanNameIsNull(limit: Int): List<Stock> =
        queryFactory
            .selectFrom(stock)
            .where(stock.koreanName.isNull)
            .limit(limit.toLong())
            .fetch()

    override fun search(keyword: String?, pageable: Pageable): Page<Stock> {
        val content =
            queryFactory
                .selectFrom(stock)
                .where(keywordContains(keyword))
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
}
