package org.kjs.stocknews.controller

import jakarta.servlet.http.HttpSession
import org.kjs.stocknews.common.currentUserId
import org.kjs.stocknews.model.dto.RegisterUserStockRequest
import org.kjs.stocknews.model.dto.UserStockResponse
import org.kjs.stocknews.service.UserStockService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users/me/stocks")
class UserStockController(
    private val userStockService: UserStockService,
) {
    @PostMapping
    fun register(@RequestBody request: RegisterUserStockRequest, session: HttpSession) {
        userStockService.register(session.currentUserId(), request.stockIds)
    }

    @GetMapping
    fun list(session: HttpSession): List<UserStockResponse> =
        userStockService.list(session.currentUserId())

    @DeleteMapping("/{stockId}")
    fun unregister(@PathVariable stockId: Long, session: HttpSession) {
        userStockService.unregister(session.currentUserId(), stockId)
    }
}
