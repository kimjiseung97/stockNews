package org.kjs.stocknews.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpSession
import org.kjs.stocknews.common.currentUserId
import org.kjs.stocknews.model.dto.RegisterUserStockRequest
import org.kjs.stocknews.model.dto.UnregisterUserStockRequest
import org.kjs.stocknews.model.dto.UserStockResponse
import org.kjs.stocknews.service.UserStockService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "UserStock", description = "로그인한 사용자의 관심종목 등록/조회/해제 API")
@RestController
@RequestMapping("/users/me/stocks")
class UserStockController(
    private val userStockService: UserStockService,
) {
    @Operation(summary = "관심종목 등록", description = "현재 로그인한 사용자의 관심종목으로 지정한 종목 ID들을 등록한다.")
    @PostMapping
    fun register(@RequestBody request: RegisterUserStockRequest, session: HttpSession) {
        userStockService.register(session.currentUserId(), request.stockIds)
    }

    @Operation(summary = "관심종목 조회", description = "현재 로그인한 사용자가 등록한 관심종목 목록을 페이징 조회한다.")
    @GetMapping
    fun list(session: HttpSession, pageable: Pageable): Page<UserStockResponse> =
        userStockService.list(session.currentUserId(), pageable)

    @Operation(summary = "관심종목 해제", description = "현재 로그인한 사용자의 관심종목에서 지정한 종목 ID들을 제거한다.")
    @DeleteMapping
    fun unregister(@RequestBody request: UnregisterUserStockRequest, session: HttpSession) {
        userStockService.unregister(session.currentUserId(), request.stockIds)
    }
}
