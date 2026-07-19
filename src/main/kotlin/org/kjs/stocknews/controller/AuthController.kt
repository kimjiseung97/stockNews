package org.kjs.stocknews.controller

import org.kjs.stocknews.common.ApiResponse
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
    fun signUp(@RequestBody request: SignUpRequest): ApiResponse<Unit> {
        authService.signUp(request.email, request.password)
        return ApiResponse.success(message = "인증 메일을 발송했습니다")
    }

    @PostMapping("/verify")
    fun verifyEmail(@RequestBody request: VerifyEmailRequest): ApiResponse<Unit> {
        authService.verifyEmail(request.email, request.code)
        return ApiResponse.success(message = "회원가입이 완료되었습니다")
    }
}
