package org.kjs.stocknews.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.kjs.stocknews.model.dto.StockResponse
import org.kjs.stocknews.service.StockService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Stock", description = "종목 검색 API")
@RestController
@RequestMapping("/stocks")
class StockController(
    private val stockService: StockService,
) {
    @Operation(summary = "종목 검색", description = "키워드(티커/종목명)로 종목을 페이징 검색한다. 키워드가 없으면 전체 목록을 반환한다.")
    @GetMapping
    fun search(@RequestParam(required = false) keyword: String?, pageable: Pageable): Page<StockResponse> =
        stockService.search(keyword, pageable)
}
