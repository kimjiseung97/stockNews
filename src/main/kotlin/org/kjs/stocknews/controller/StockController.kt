package org.kjs.stocknews.controller

import org.kjs.stocknews.model.dto.StockResponse
import org.kjs.stocknews.service.StockService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/stocks")
class StockController(
    private val stockService: StockService,
) {
    @GetMapping
    fun search(@RequestParam(required = false) keyword: String?, pageable: Pageable): Page<StockResponse> =
        stockService.search(keyword, pageable)
}
