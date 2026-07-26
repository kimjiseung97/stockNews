package org.kjs.stocknews.controller

import jakarta.servlet.http.HttpSession
import org.kjs.stocknews.common.SessionKeys
import org.kjs.stocknews.model.dto.LoginRequest
import org.kjs.stocknews.model.dto.SignUpRequest
import org.kjs.stocknews.model.dto.VerifyEmailRequest
import org.kjs.stocknews.service.AuthService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/signup")
    fun signUp(@RequestBody request: SignUpRequest) {
        authService.signUp(request.email, request.password)
    }

    @PostMapping("/verify")
    fun verifyEmail(@RequestBody request: VerifyEmailRequest) {
        authService.verifyEmail(request.email, request.code)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest, session: HttpSession) {
        val userId = authService.login(request.email, request.password)
        session.setAttribute(SessionKeys.USER_ID, userId)
    }

    @PostMapping("/logout")
    fun logout(session: HttpSession) {
        session.invalidate()
    }
}
