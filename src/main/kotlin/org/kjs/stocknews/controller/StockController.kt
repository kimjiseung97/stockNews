package org.kjs.stocknews.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.kjs.stocknews.model.dto.PopularStockResponse
import org.kjs.stocknews.model.dto.StockDetailResponse
import org.kjs.stocknews.model.dto.StockNewsResponse
import org.kjs.stocknews.model.dto.StockResponse
import org.kjs.stocknews.service.StockService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Stock", description = "종목 검색 API")
@RestController
@RequestMapping("/stocks")
@Validated
class StockController(
    private val stockService: StockService,
) {
    @Operation(summary = "종목 검색", description = "키워드(티커/종목명)로 종목을 페이징 검색한다. 키워드가 없으면 전체 목록을 반환한다.")
    @GetMapping
    fun search(@RequestParam(required = false) keyword: String?, pageable: Pageable): Page<StockResponse> =
        stockService.search(keyword, pageable)

    @Operation(summary = "종목 상세정보 조회", description = "종목 PK로 기업개요/CEO명/국적/도시/홈페이지/업종/상장일 등 상세정보를 조회한다. 로그인 불필요.")
    @GetMapping("/detail")
    fun getDetail(@RequestParam("stockId") stockId: Long): StockDetailResponse =
        stockService.getDetail(stockId)

    @Operation(summary = "인기종목 조회", description = "검색/상세조회 통계(TB_STOCK_SEARCH_COUNT) 기준 상위 N개 종목을 반환한다.")
    @GetMapping("/popular")
    fun getPopularStocks(
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) limit: Int,
    ): List<PopularStockResponse> = stockService.getPopularStocks(limit)

    @Operation(summary = "종목별 뉴스 목록 조회", description = "종목 PK로 수집된 뉴스를 최신순으로 페이징 조회한다. 로그인 불필요.")
    @GetMapping("/{stockId}/news")
    fun getNews(@PathVariable stockId: Long, pageable: Pageable): Page<StockNewsResponse> =
        stockService.getNews(stockId, pageable)
}
