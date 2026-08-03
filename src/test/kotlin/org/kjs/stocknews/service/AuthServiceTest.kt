package org.kjs.stocknews.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.table.User
import org.kjs.stocknews.model.table.VerificationPurpose
import org.kjs.stocknews.repository.EmailVerificationRepository
import org.kjs.stocknews.repository.UserRepository
import org.kjs.stocknews.repository.VerificationRepository
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder

class AuthServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val emailVerificationRepository = mock(EmailVerificationRepository::class.java)
    private val verificationRepository = mock(VerificationRepository::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)
    private val verificationMailSender = mock(VerificationMailSender::class.java)
    private val authService = AuthService(
        userRepository,
        emailVerificationRepository,
        verificationRepository,
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
        val verification = org.kjs.stocknews.model.table.Verification(
            identifier = recoveryEmail,
            purpose = VerificationPurpose.FIND_EMAIL,
            code = code,
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5),
        )
        `when`(verificationRepository.findByIdentifierAndPurpose(recoveryEmail, VerificationPurpose.FIND_EMAIL))
            .thenReturn(verification)
        `when`(userRepository.findByRecoveryEmail(recoveryEmail)).thenReturn(
            User(email = "user@example.com", password = "encoded", recoveryEmail = recoveryEmail),
        )

        val result = authService.verifyFindEmail(recoveryEmail, code)

        assert(result == "user@example.com")
    }

    @Test
    fun `등록되지 않은 이메일로 비밀번호 재설정을 요청하면 EMAIL_NOT_FOUND 예외가 발생한다`() {
        `when`(userRepository.findByEmail("user@example.com")).thenReturn(null)

        val exception =
            assertThrows<BusinessException> { authService.requestResetPassword("user@example.com") }
        assert(exception.resultCode == ResultCode.EMAIL_NOT_FOUND)
    }

    @Test
    fun `복구 이메일이 없는 계정이 비밀번호 재설정을 요청하면 RECOVERY_EMAIL_NOT_FOUND 예외가 발생한다`() {
        `when`(userRepository.findByEmail("user@example.com")).thenReturn(
            User(email = "user@example.com", password = "encoded", recoveryEmail = null),
        )

        val exception =
            assertThrows<BusinessException> { authService.requestResetPassword("user@example.com") }
        assert(exception.resultCode == ResultCode.RECOVERY_EMAIL_NOT_FOUND)
    }

    @Test
    fun `등록된 계정이 비밀번호 재설정을 요청하면 복구 이메일로 인증코드가 발송된다`() {
        `when`(userRepository.findByEmail("user@example.com")).thenReturn(
            User(email = "user@example.com", password = "encoded", recoveryEmail = "recovery@example.com"),
        )

        authService.requestResetPassword("user@example.com")

        org.mockito.Mockito.verify(verificationMailSender).sendResetPasswordCode(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyLong(),
        )
    }

    @Test
    fun `인증코드가 일치하면 비밀번호 재설정 확인 시 임시 비밀번호가 발급되고 발송된다`() {
        val email = "user@example.com"
        val code = "123456"
        val verification = org.kjs.stocknews.model.table.Verification(
            identifier = email,
            purpose = VerificationPurpose.RESET_PASSWORD,
            code = code,
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5),
        )
        val user = User(email = email, password = "encoded", recoveryEmail = "recovery@example.com")
        `when`(verificationRepository.findByIdentifierAndPurpose(email, VerificationPurpose.RESET_PASSWORD))
            .thenReturn(verification)
        `when`(userRepository.findByEmail(email)).thenReturn(user)
        `when`(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString())).thenReturn("new-encoded")

        authService.confirmResetPassword(email, code)

        assert(user.password == "new-encoded")
        assert(user.temporaryPassword)
        org.mockito.Mockito.verify(verificationMailSender).sendTemporaryPassword(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
        )
    }

    @Test
    fun `인증코드가 일치하지 않으면 비밀번호 재설정 확인 시 VERIFICATION_CODE_MISMATCH 예외가 발생한다`() {
        val email = "user@example.com"
        val verification = org.kjs.stocknews.model.table.Verification(
            identifier = email,
            purpose = VerificationPurpose.RESET_PASSWORD,
            code = "123456",
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5),
        )
        `when`(verificationRepository.findByIdentifierAndPurpose(email, VerificationPurpose.RESET_PASSWORD))
            .thenReturn(verification)

        val exception =
            assertThrows<BusinessException> { authService.confirmResetPassword(email, "999999") }
        assert(exception.resultCode == ResultCode.VERIFICATION_CODE_MISMATCH)
    }

    @Test
    fun `임시 비밀번호로 로그인하면 requiresPasswordChange가 true로 반환된다`() {
        val user = mock(User::class.java)
        `when`(user.id).thenReturn(1L)
        `when`(user.password).thenReturn("encoded")
        `when`(user.temporaryPassword).thenReturn(true)
        `when`(userRepository.findByEmail("user@example.com")).thenReturn(user)
        `when`(passwordEncoder.matches("password1!", "encoded")).thenReturn(true)

        val result = authService.login("user@example.com", "password1!")

        assert(result.requiresPasswordChange)
    }

    @Test
    fun `일반 비밀번호로 로그인하면 requiresPasswordChange가 false로 반환된다`() {
        val user = mock(User::class.java)
        `when`(user.id).thenReturn(1L)
        `when`(user.password).thenReturn("encoded")
        `when`(user.temporaryPassword).thenReturn(false)
        `when`(userRepository.findByEmail("user@example.com")).thenReturn(user)
        `when`(passwordEncoder.matches("password1!", "encoded")).thenReturn(true)

        val result = authService.login("user@example.com", "password1!")

        assert(!result.requiresPasswordChange)
    }

    @Test
    fun `현재 비밀번호가 일치하지 않으면 비밀번호 변경 시 CURRENT_PASSWORD_MISMATCH 예외가 발생한다`() {
        val user = User(email = "user@example.com", password = "encoded", recoveryEmail = "recovery@example.com")
        `when`(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user))
        `when`(passwordEncoder.matches("wrong-password", "encoded")).thenReturn(false)

        val exception =
            assertThrows<BusinessException> { authService.changePassword(1L, "wrong-password", "new-password1!") }
        assert(exception.resultCode == ResultCode.CURRENT_PASSWORD_MISMATCH)
    }

    @Test
    fun `현재 비밀번호가 일치하면 비밀번호가 변경되고 임시 비밀번호 상태가 해제된다`() {
        val user = User(email = "user@example.com", password = "encoded", recoveryEmail = "recovery@example.com")
        user.temporaryPassword = true
        `when`(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user))
        `when`(passwordEncoder.matches("current-password1!", "encoded")).thenReturn(true)
        `when`(passwordEncoder.encode("new-password1!")).thenReturn("new-encoded")

        authService.changePassword(1L, "current-password1!", "new-password1!")

        assert(user.password == "new-encoded")
        assert(!user.temporaryPassword)
    }
}
