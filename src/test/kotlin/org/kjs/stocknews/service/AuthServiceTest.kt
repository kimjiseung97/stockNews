package org.kjs.stocknews.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.repository.EmailVerificationRepository
import org.kjs.stocknews.repository.UserRepository
import org.mockito.Mockito.mock
import org.springframework.security.crypto.password.PasswordEncoder

class AuthServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val emailVerificationRepository = mock(EmailVerificationRepository::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)
    private val verificationMailSender = mock(VerificationMailSender::class.java)
    private val authService = AuthService(
        userRepository,
        emailVerificationRepository,
        passwordEncoder,
        verificationMailSender,
        codeLength = 6,
        expiryMinutes = 10,
    )

    @Test
    fun `이메일이 공백이면 회원가입 시 EMAIL_REQUIRED 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> { authService.signUp(" ", "password1!") }
        assert(exception.resultCode == ResultCode.EMAIL_REQUIRED)
    }

    @Test
    fun `이메일 형식이 올바르지 않으면 회원가입 시 INVALID_EMAIL_FORMAT 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> { authService.signUp("not-an-email", "password1!") }
        assert(exception.resultCode == ResultCode.INVALID_EMAIL_FORMAT)
    }

    @Test
    fun `이메일이 50자를 초과하면 회원가입 시 EMAIL_TOO_LONG 예외가 발생한다`() {
        val tooLongEmail = "a".repeat(45) + "@test.com"
        val exception = assertThrows<BusinessException> { authService.signUp(tooLongEmail, "password1!") }
        assert(exception.resultCode == ResultCode.EMAIL_TOO_LONG)
    }

    @Test
    fun `비밀번호가 최대 길이를 초과하면 회원가입 시 INVALID_PASSWORD_LENGTH 예외가 발생한다`() {
        val tooLongPassword = "a".repeat(21)
        val exception = assertThrows<BusinessException> { authService.signUp("user@example.com", tooLongPassword) }
        assert(exception.resultCode == ResultCode.INVALID_PASSWORD_LENGTH)
    }

    @Test
    fun `비밀번호가 최소 길이 미만이면 로그인 시 INVALID_PASSWORD_LENGTH 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> { authService.login("user@example.com", "short1!") }
        assert(exception.resultCode == ResultCode.INVALID_PASSWORD_LENGTH)
    }

    @Test
    fun `인증코드 길이가 올바르지 않으면 이메일 인증 시 INVALID_VERIFICATION_CODE_LENGTH 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> { authService.verifyEmail("user@example.com", "12") }
        assert(exception.resultCode == ResultCode.INVALID_VERIFICATION_CODE_LENGTH)
    }
}
