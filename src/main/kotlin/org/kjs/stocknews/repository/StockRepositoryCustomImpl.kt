package org.kjs.stocknews.repository

import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.ComparablePath
import com.querydsl.core.types.dsl.PathBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.kjs.stocknews.model.dto.PopularStockResponse
import org.kjs.stocknews.model.table.QStock.stock
import org.kjs.stocknews.model.table.QStockDetail.stockDetail
import org.kjs.stocknews.model.table.QStockSearchCount.stockSearchCount
import org.kjs.stocknews.model.table.Stock
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository
import java.time.LocalDate

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

    // limit을 두지 않고 koreanName이 비어있는 종목 전체를 한 번에 가져온다. limit(batch-size)을 두면
    // "ID 오름차순으로 N개"를 매 실행마다 다시 뽑는 꼴이 되어, 네이버에서 영구히 검색 안 되는 OTC/해외 ADR
    // 종목이 항상 그 N개 안에 껴서 뒤쪽(ID 큰) 종목이 영원히 시도조차 안 되는 문제가 있었다(실측: JPM/TSM처럼
    // 정상적으로 풀리는 종목도 순서가 안 와서 방치됨). 전체를 한 번에 가져오면 그런 종목이 매번 끼어도
    // 뒤쪽까지 전부 시도는 되므로 안전하다 - 청크 커밋(ENRICH_CHUNK_SIZE)으로 중간 진행 상황은 계속 저장된다.
    override fun findByKoreanNameIsNull(): List<Stock> =
        queryFactory
            .selectFrom(stock)
            .where(stock.koreanName.isNull)
            .fetch()

    // 토스 전용으로 유입돼 영문명을 못 채운 종목은 name에 임시로 ticker를 넣어뒀다(StockSeedJobConfig 참고) - 이 마커로 대상 조회.
    override fun findByNameEqualsTicker(limit: Int): List<Stock> =
        queryFactory
            .selectFrom(stock)
            .where(stock.name.eq(stock.ticker))
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

    // TB_STOCK_SEARCH_COUNT를 STOCK_ID로 groupBy 후 검색건수 내림차순 정렬 - 금일(00:00~다음날 00:00 미만) 인기종목 Top N 조회.
    override fun findPopularStocks(limit: Int): List<PopularStockResponse> {
        val todayStart = LocalDate.now().atStartOfDay()
        val todayEnd = todayStart.plusDays(1)

        return queryFactory
            .select(
                Projections.constructor(
                    PopularStockResponse::class.java,
                    stock.id,
                    stock.ticker,
                    stock.name,
                    stock.theme,
                    stock.koreanName,
                    stockSearchCount.id.count(),
                ),
            )
            .from(stockSearchCount)
            .join(stock).on(stockSearchCount.stockId.eq(stock.id))
            .where(stockSearchCount.createdAt.goe(todayStart), stockSearchCount.createdAt.lt(todayEnd))
            .groupBy(stock.id)
            .orderBy(stockSearchCount.id.count().desc())
            .limit(limit.toLong())
            .fetch()
    }

    // 한글명 부분일치 또는 티커 부분일치(대소문자 무관) - VOO처럼 koreanName 보강이 안 된 종목도 티커로는 찾을 수 있도록.
    private fun keywordContains(keyword: String?): BooleanExpression? {
        val trimmed = keyword?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return stock.koreanName.contains(trimmed).or(stock.ticker.containsIgnoreCase(trimmed))
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
