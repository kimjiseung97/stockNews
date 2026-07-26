package org.kjs.stocknews.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.table.User
import org.kjs.stocknews.repository.EmailVerificationRepository
import org.kjs.stocknews.repository.FindEmailVerificationRepository
import org.kjs.stocknews.repository.UserRepository
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder

class AuthServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val emailVerificationRepository = mock(EmailVerificationRepository::class.java)
    private val findEmailVerificationRepository = mock(FindEmailVerificationRepository::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)
    private val verificationMailSender = mock(VerificationMailSender::class.java)
    private val authService = AuthService(
        userRepository,
        emailVerificationRepository,
        findEmailVerificationRepository,
        passwordEncoder,
        verificationMailSender,
        codeLength = 6,
        expiryMinutes = 10,
    )

    @Test
    fun `이메일이 공백이면 회원가입 시 EMAIL_REQUIRED 예외가 발생한다`() {
        val exception =
            assertThrows<BusinessException> { authService.signUp(" ", "password1!", "recovery@example.com") }
        assert(exception.resultCode == ResultCode.EMAIL_REQUIRED)
    }

    @Test
    fun `이메일 형식이 올바르지 않으면 회원가입 시 INVALID_EMAIL_FORMAT 예외가 발생한다`() {
        val exception =
            assertThrows<BusinessException> {
                authService.signUp("not-an-email", "password1!", "recovery@example.com")
            }
        assert(exception.resultCode == ResultCode.INVALID_EMAIL_FORMAT)
    }

    @Test
    fun `이메일이 50자를 초과하면 회원가입 시 EMAIL_TOO_LONG 예외가 발생한다`() {
        val tooLongEmail = "a".repeat(45) + "@test.com"
        val exception =
            assertThrows<BusinessException> {
                authService.signUp(tooLongEmail, "password1!", "recovery@example.com")
            }
        assert(exception.resultCode == ResultCode.EMAIL_TOO_LONG)
    }

    @Test
    fun `비밀번호가 최대 길이를 초과하면 회원가입 시 INVALID_PASSWORD_LENGTH 예외가 발생한다`() {
        val tooLongPassword = "a".repeat(21)
        val exception =
            assertThrows<BusinessException> {
                authService.signUp("user@example.com", tooLongPassword, "recovery@example.com")
            }
        assert(exception.resultCode == ResultCode.INVALID_PASSWORD_LENGTH)
    }

    @Test
    fun `복구 이메일이 공백이면 회원가입 시 RECOVERY_EMAIL_REQUIRED 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> { authService.signUp("user@example.com", "password1!", " ") }
        assert(exception.resultCode == ResultCode.RECOVERY_EMAIL_REQUIRED)
    }

    @Test
    fun `복구 이메일이 로그인 이메일과 같으면 회원가입 시 RECOVERY_EMAIL_SAME_AS_EMAIL 예외가 발생한다`() {
        val exception =
            assertThrows<BusinessException> {
                authService.signUp("user@example.com", "password1!", "user@example.com")
            }
        assert(exception.resultCode == ResultCode.RECOVERY_EMAIL_SAME_AS_EMAIL)
    }

    @Test
    fun `이미 사용 중인 복구 이메일이면 회원가입 시 RECOVERY_EMAIL_ALREADY_REGISTERED 예외가 발생한다`() {
        `when`(userRepository.existsByRecoveryEmail("recovery@example.com")).thenReturn(true)

        val exception =
            assertThrows<BusinessException> {
                authService.signUp("user@example.com", "password1!", "recovery@example.com")
            }
        assert(exception.resultCode == ResultCode.RECOVERY_EMAIL_ALREADY_REGISTERED)
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

    @Test
    fun `등록되지 않은 복구 이메일로 이메일 찾기를 요청하면 RECOVERY_EMAIL_NOT_FOUND 예외가 발생한다`() {
        `when`(userRepository.existsByRecoveryEmail("recovery@example.com")).thenReturn(false)

        val exception =
            assertThrows<BusinessException> { authService.requestFindEmail("recovery@example.com") }
        assert(exception.resultCode == ResultCode.RECOVERY_EMAIL_NOT_FOUND)
    }

    @Test
    fun `등록된 복구 이메일로 이메일 찾기를 요청하면 인증코드가 발송된다`() {
        `when`(userRepository.existsByRecoveryEmail("recovery@example.com")).thenReturn(true)

        authService.requestFindEmail("recovery@example.com")

        org.mockito.Mockito.verify(verificationMailSender).sendFindEmailCode(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyLong(),
        )
    }

    @Test
    fun `인증코드가 일치하면 이메일 찾기 검증 시 가입 이메일을 반환한다`() {
        val recoveryEmail = "recovery@example.com"
        val code = "123456"
        val verification = org.kjs.stocknews.model.table.FindEmailVerification(
            recoveryEmail = recoveryEmail,
            code = code,
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5),
        )
        `when`(findEmailVerificationRepository.findById(recoveryEmail)).thenReturn(java.util.Optional.of(verification))
        `when`(userRepository.findByRecoveryEmail(recoveryEmail)).thenReturn(
            User(email = "user@example.com", password = "encoded", recoveryEmail = recoveryEmail),
        )

        val result = authService.verifyFindEmail(recoveryEmail, code)

        assert(result == "user@example.com")
    }
}
