package org.kjs.stocknews.controller

import jakarta.servlet.http.HttpSession
import org.kjs.stocknews.common.SessionKeys
import org.kjs.stocknews.model.dto.FindEmailRequest
import org.kjs.stocknews.model.dto.FindEmailResponse
import org.kjs.stocknews.model.dto.LoginRequest
import org.kjs.stocknews.model.dto.SignUpRequest
import org.kjs.stocknews.model.dto.VerifyEmailRequest
import org.kjs.stocknews.model.dto.VerifyFindEmailRequest
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
        authService.signUp(request.email, request.password, request.recoveryEmail)
    }

    @PostMapping("/verify")
    fun verifyEmail(@RequestBody request: VerifyEmailRequest) {
        authService.verifyEmail(request.email, request.code)
    }

    @PostMapping("/find-email/request")
    fun requestFindEmail(@RequestBody request: FindEmailRequest) {
        authService.requestFindEmail(request.recoveryEmail)
    }

    @PostMapping("/find-email/verify")
    fun verifyFindEmail(@RequestBody request: VerifyFindEmailRequest): FindEmailResponse =
        FindEmailResponse(authService.verifyFindEmail(request.recoveryEmail, request.code))

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
