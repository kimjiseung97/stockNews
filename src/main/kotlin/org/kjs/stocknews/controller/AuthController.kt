package org.kjs.stocknews.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpSession
import org.kjs.stocknews.common.SessionKeys
import org.kjs.stocknews.common.currentUserId
import org.kjs.stocknews.model.dto.ChangePasswordRequest
import org.kjs.stocknews.model.dto.EmailExistsResponse
import org.kjs.stocknews.model.dto.FindEmailRequest
import org.kjs.stocknews.model.dto.FindEmailResponse
import org.kjs.stocknews.model.dto.LoginRequest
import org.kjs.stocknews.model.dto.LoginResponse
import org.kjs.stocknews.model.dto.ResetPasswordRequest
import org.kjs.stocknews.model.dto.SignUpRequest
import org.kjs.stocknews.model.dto.VerifyEmailRequest
import org.kjs.stocknews.model.dto.VerifyFindEmailRequest
import org.kjs.stocknews.model.dto.VerifyResetPasswordRequest
import org.kjs.stocknews.service.AuthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Auth", description = "회원가입, 이메일 인증, 로그인/로그아웃, 비밀번호 찾기·변경 API")
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {
    @Operation(
        summary = "이메일 중복 확인 및 인증코드 발송",
        description = "이메일 중복 여부를 확인하고, 사용 가능한 이메일이면 곧바로 인증코드를 발송한다. 메일 발송 성공 여부를 isMailSendSuccess로 반환한다.",
    )
    @GetMapping("/email/exists")
    fun checkEmailDuplicate(@RequestParam email: String): EmailExistsResponse {
        val result = authService.checkEmailDuplicate(email)
        return EmailExistsResponse(result.duplicated, result.isMailSendSuccess)
    }

    @Operation(summary = "회원가입 - 이메일 인증코드 확인", description = "이메일과 인증코드를 함께 검증하여 이메일 인증을 완료 처리한다(계정은 아직 생성하지 않음).")
    @PostMapping("/verify")
    fun verifyEmail(@RequestBody request: VerifyEmailRequest) {
        authService.verifyEmail(request.email, request.code)
    }

    @Operation(
        summary = "회원가입 - 최종 완료",
        description = "이메일 인증이 완료된 이메일에 대해 비밀번호/복구용 이메일을 받아 계정 생성을 완료한다.",
    )
    @PostMapping("/signup/complete")
    fun completeSignUp(@RequestBody request: SignUpRequest) {
        authService.completeSignUp(request.email, request.password, request.recoveryEmail)
    }

    @Operation(summary = "이메일 찾기 요청", description = "복구용 이메일로 가입된 이메일 확인용 인증코드를 발송한다.")
    @PostMapping("/find-email/request")
    fun requestFindEmail(@RequestBody request: FindEmailRequest) {
        authService.requestFindEmail(request.recoveryEmail)
    }

    @Operation(summary = "이메일 찾기 인증", description = "인증코드를 검증하고 마스킹된 가입 이메일을 반환한다.")
    @PostMapping("/find-email/verify")
    fun verifyFindEmail(@RequestBody request: VerifyFindEmailRequest): FindEmailResponse =
        FindEmailResponse(authService.verifyFindEmail(request.recoveryEmail, request.code))

    @Operation(summary = "비밀번호 재설정 요청", description = "가입 이메일로 비밀번호 재설정용 인증코드를 발송한다.")
    @PostMapping("/reset-password/request")
    fun requestResetPassword(@RequestBody request: ResetPasswordRequest) {
        authService.requestResetPassword(request.email)
    }

    @Operation(summary = "비밀번호 재설정 확인", description = "인증코드를 검증하고 임시 비밀번호를 발급해 재설정을 완료한다.")
    @PostMapping("/reset-password/confirm")
    fun confirmResetPassword(@RequestBody request: VerifyResetPasswordRequest) {
        authService.confirmResetPassword(request.email, request.code)
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 세션을 생성한다.")
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest, session: HttpSession): LoginResponse {
        val result = authService.login(request.email, request.password)
        session.setAttribute(SessionKeys.USER_ID, result.userId)
        return LoginResponse(result.email, result.requiresPasswordChange)
    }

    @Operation(summary = "비밀번호 변경", description = "로그인한 사용자의 현재 비밀번호를 확인 후 새 비밀번호로 변경한다.")
    @PutMapping("/password")
    fun changePassword(@RequestBody request: ChangePasswordRequest, session: HttpSession) {
        authService.changePassword(session.currentUserId(), request.currentPassword, request.newPassword)
    }

    @Operation(summary = "로그아웃", description = "현재 세션을 무효화한다.")
    @PostMapping("/logout")
    fun logout(session: HttpSession) {
        session.invalidate()
    }
}
