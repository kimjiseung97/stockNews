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

// Mockito의 eq()/any()는 널을 반환해 Kotlin non-null 파라미터 콜사이트에서 NPE를 유발한다.
// 로컬 래퍼로 정적 반환 타입을 non-null로 감춰서 우회한다(널리 알려진 Kotlin+Mockito 관용구).
private fun <T> eqArg(value: T): T = org.mockito.ArgumentMatchers.eq(value)
private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()

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
        maxRequestsPerWindow = 3,
        maxAttempts = 3,
    )

    @Test
    fun `이미 가입된 이메일이면 중복 확인 시 duplicated가 true이고 인증코드는 발송되지 않는다`() {
        `when`(userRepository.existsByEmail("user@example.com")).thenReturn(true)

        val result = authService.checkEmailDuplicate("user@example.com")

        assert(result.duplicated)
        assert(!result.isMailSendSuccess)
        org.mockito.Mockito.verify(verificationMailSender, org.mockito.Mockito.never()).sendVerificationCode(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyLong(),
        )
    }

    @Test
    fun `가입되지 않은 이메일이면 중복 확인 시 duplicated가 false이고 인증코드가 발송된다`() {
        `when`(userRepository.existsByEmail("user@example.com")).thenReturn(false)

        val result = authService.checkEmailDuplicate("user@example.com")

        assert(!result.duplicated)
        assert(result.isMailSendSuccess)
        org.mockito.Mockito.verify(verificationMailSender).sendVerificationCode(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyLong(),
        )
    }

    @Test
    fun `인증코드 메일 발송에 실패하면 중복 확인 시 isMailSendSuccess가 false를 반환한다`() {
        `when`(userRepository.existsByEmail("user@example.com")).thenReturn(false)
        org.mockito.Mockito.doThrow(org.springframework.mail.MailSendException("mail server down"))
            .`when`(verificationMailSender)
            .sendVerificationCode(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
            )

        val result = authService.checkEmailDuplicate("user@example.com")

        assert(!result.duplicated)
        assert(!result.isMailSendSuccess)
    }

    @Test
    fun `10분 내 인증코드 요청이 3회 이상이면 중복 확인 시 VERIFICATION_REQUEST_LIMIT_EXCEEDED 예외가 발생한다`() {
        `when`(userRepository.existsByEmail("user@example.com")).thenReturn(false)
        `when`(
            emailVerificationRepository.countByEmailAndCreatedAtAfter(
                eqArg("user@example.com"),
                anyArg(),
            ),
        ).thenReturn(3L)

        val exception = assertThrows<BusinessException> { authService.checkEmailDuplicate("user@example.com") }
        assert(exception.resultCode == ResultCode.VERIFICATION_REQUEST_LIMIT_EXCEEDED)
        org.mockito.Mockito.verify(verificationMailSender, org.mockito.Mockito.never()).sendVerificationCode(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyLong(),
        )
    }

    @Test
    fun `이메일 형식이 올바르지 않으면 중복 확인 시 INVALID_EMAIL_FORMAT 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> { authService.checkEmailDuplicate("not-an-email") }
        assert(exception.resultCode == ResultCode.INVALID_EMAIL_FORMAT)
    }

    @Test
    fun `이메일이 공백이면 중복 확인 시 EMAIL_REQUIRED 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> { authService.checkEmailDuplicate(" ") }
        assert(exception.resultCode == ResultCode.EMAIL_REQUIRED)
    }

    @Test
    fun `이메일이 50자를 초과하면 중복 확인 시 EMAIL_TOO_LONG 예외가 발생한다`() {
        val tooLongEmail = "a".repeat(45) + "@test.com"
        val exception = assertThrows<BusinessException> { authService.checkEmailDuplicate(tooLongEmail) }
        assert(exception.resultCode == ResultCode.EMAIL_TOO_LONG)
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
    fun `인증코드가 일치하면 이메일 인증 시 verified 상태로 저장되고 계정은 생성되지 않는다`() {
        val email = "user@example.com"
        val verification = org.kjs.stocknews.model.table.EmailVerification(
            email = email,
            code = "123456",
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5),
        )
        `when`(emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)).thenReturn(verification)

        authService.verifyEmail(email, "123456")

        assert(verification.verified)
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any())
        org.mockito.Mockito.verify(emailVerificationRepository, org.mockito.Mockito.never()).delete(verification)
    }

    @Test
    fun `인증코드가 일치하지 않으면 이메일 인증 시 attemptCount가 증가하고 VERIFICATION_CODE_MISMATCH 예외가 발생한다`() {
        val email = "user@example.com"
        val verification = org.kjs.stocknews.model.table.EmailVerification(
            email = email,
            code = "123456",
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5),
        )
        `when`(emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)).thenReturn(verification)

        val exception = assertThrows<BusinessException> { authService.verifyEmail(email, "000000") }

        assert(exception.resultCode == ResultCode.VERIFICATION_CODE_MISMATCH)
        assert(verification.attemptCount == 1)
    }

    @Test
    fun `이메일 인증 시도 횟수가 초과되면 VERIFICATION_ATTEMPTS_EXCEEDED 예외가 발생한다`() {
        val email = "user@example.com"
        val verification = org.kjs.stocknews.model.table.EmailVerification(
            email = email,
            code = "123456",
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5),
            attemptCount = 3,
        )
        `when`(emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)).thenReturn(verification)

        val exception = assertThrows<BusinessException> { authService.verifyEmail(email, "123456") }
        assert(exception.resultCode == ResultCode.VERIFICATION_ATTEMPTS_EXCEEDED)
    }

    @Test
    fun `이메일 찾기 검증 시도 횟수가 초과되면 VERIFICATION_ATTEMPTS_EXCEEDED 예외가 발생한다`() {
        val recoveryEmail = "recovery@example.com"
        val verification = org.kjs.stocknews.model.table.Verification(
            identifier = recoveryEmail,
            purpose = VerificationPurpose.FIND_EMAIL,
            code = "123456",
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5),
            attemptCount = 3,
        )
        `when`(
            verificationRepository.findTopByIdentifierAndPurposeOrderByCreatedAtDesc(
                recoveryEmail,
                VerificationPurpose.FIND_EMAIL,
            ),
        ).thenReturn(verification)

        val exception = assertThrows<BusinessException> { authService.verifyFindEmail(recoveryEmail, "123456") }
        assert(exception.resultCode == ResultCode.VERIFICATION_ATTEMPTS_EXCEEDED)
    }

    @Test
    fun `비밀번호 재설정 확인 시도 횟수가 초과되면 VERIFICATION_ATTEMPTS_EXCEEDED 예외가 발생한다`() {
        val email = "user@example.com"
        val verification = org.kjs.stocknews.model.table.Verification(
            identifier = email,
            purpose = VerificationPurpose.RESET_PASSWORD,
            code = "123456",
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5),
            attemptCount = 3,
        )
        `when`(
            verificationRepository.findTopByIdentifierAndPurposeOrderByCreatedAtDesc(
                email,
                VerificationPurpose.RESET_PASSWORD,
            ),
        ).thenReturn(verification)

        val exception = assertThrows<BusinessException> { authService.confirmResetPassword(email, "123456") }
        assert(exception.resultCode == ResultCode.VERIFICATION_ATTEMPTS_EXCEEDED)
    }

    @Test
    fun `이메일 인증을 완료하지 않았으면 회원가입 완료 시 EMAIL_NOT_VERIFIED 예외가 발생한다`() {
        val email = "user@example.com"
        val verification = org.kjs.stocknews.model.table.EmailVerification(
            email = email,
            code = "123456",
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5),
            verified = false,
        )
        `when`(emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)).thenReturn(verification)

        val exception =
            assertThrows<BusinessException> {
                authService.completeSignUp(email, "password1!", "recovery@example.com")
            }
        assert(exception.resultCode == ResultCode.EMAIL_NOT_VERIFIED)
    }

    @Test
    fun `이메일 인증이 완료되어 있으면 회원가입 완료 시 계정이 생성된다`() {
        val email = "user@example.com"
        val verification = org.kjs.stocknews.model.table.EmailVerification(
            email = email,
            code = "123456",
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5),
            verified = true,
        )
        `when`(emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)).thenReturn(verification)
        `when`(passwordEncoder.encode("password1!")).thenReturn("encoded-password")

        authService.completeSignUp(email, "password1!", "recovery@example.com")

        org.mockito.Mockito.verify(userRepository).save(org.mockito.ArgumentMatchers.any())
        org.mockito.Mockito.verify(emailVerificationRepository).deleteByEmail(email)
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
    fun `10분 내 이메일 찾기 요청이 3회 이상이면 VERIFICATION_REQUEST_LIMIT_EXCEEDED 예외가 발생한다`() {
        `when`(userRepository.existsByRecoveryEmail("recovery@example.com")).thenReturn(true)
        `when`(
            verificationRepository.countByIdentifierAndPurposeAndCreatedAtAfter(
                eqArg("recovery@example.com"),
                eqArg(VerificationPurpose.FIND_EMAIL),
                anyArg(),
            ),
        ).thenReturn(3L)

        val exception =
            assertThrows<BusinessException> { authService.requestFindEmail("recovery@example.com") }
        assert(exception.resultCode == ResultCode.VERIFICATION_REQUEST_LIMIT_EXCEEDED)
        org.mockito.Mockito.verify(verificationMailSender, org.mockito.Mockito.never()).sendFindEmailCode(
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
        `when`(
            verificationRepository.findTopByIdentifierAndPurposeOrderByCreatedAtDesc(
                recoveryEmail,
                VerificationPurpose.FIND_EMAIL,
            ),
        ).thenReturn(verification)
        `when`(userRepository.findByRecoveryEmail(recoveryEmail)).thenReturn(
            User(email = "user@example.com", password = "encoded", recoveryEmail = recoveryEmail),
        )

        val result = authService.verifyFindEmail(recoveryEmail, code)

        assert(result == "user@example.com")
    }

    @Test
    fun `등록되지 않은 이메일로 비밀번호 재설정을 요청하면 registered false를 반환한다`() {
        `when`(userRepository.findByEmail("user@example.com")).thenReturn(null)

        val registered = authService.requestResetPassword("user@example.com")

        assert(!registered)
        org.mockito.Mockito.verify(verificationMailSender, org.mockito.Mockito.never()).sendResetPasswordCode(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyLong(),
        )
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

        val registered = authService.requestResetPassword("user@example.com")

        assert(registered)
        org.mockito.Mockito.verify(verificationMailSender).sendResetPasswordCode(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyLong(),
        )
    }

    @Test
    fun `10분 내 비밀번호 재설정 요청이 3회 이상이면 VERIFICATION_REQUEST_LIMIT_EXCEEDED 예외가 발생한다`() {
        `when`(userRepository.findByEmail("user@example.com")).thenReturn(
            User(email = "user@example.com", password = "encoded", recoveryEmail = "recovery@example.com"),
        )
        `when`(
            verificationRepository.countByIdentifierAndPurposeAndCreatedAtAfter(
                eqArg("user@example.com"),
                eqArg(VerificationPurpose.RESET_PASSWORD),
                anyArg(),
            ),
        ).thenReturn(3L)

        val exception =
            assertThrows<BusinessException> { authService.requestResetPassword("user@example.com") }
        assert(exception.resultCode == ResultCode.VERIFICATION_REQUEST_LIMIT_EXCEEDED)
        org.mockito.Mockito.verify(verificationMailSender, org.mockito.Mockito.never()).sendResetPasswordCode(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyLong(),
        )
    }

    @Test
    fun `인증코드가 일치하면 비밀번호 재설정 확인 시 인증 완료 상태로 표시된다`() {
        val email = "user@example.com"
        val code = "123456"
        val verification = org.kjs.stocknews.model.table.Verification(
            identifier = email,
            purpose = VerificationPurpose.RESET_PASSWORD,
            code = code,
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5),
        )
        `when`(
            verificationRepository.findTopByIdentifierAndPurposeOrderByCreatedAtDesc(
                email,
                VerificationPurpose.RESET_PASSWORD,
            ),
        ).thenReturn(verification)

        authService.confirmResetPassword(email, code)

        assert(verification.verified)
    }

    @Test
    fun `인증이 완료되지 않은 상태로 비밀번호 재설정 완료를 요청하면 RESET_PASSWORD_NOT_VERIFIED 예외가 발생한다`() {
        val email = "user@example.com"
        val verification = org.kjs.stocknews.model.table.Verification(
            identifier = email,
            purpose = VerificationPurpose.RESET_PASSWORD,
            code = "123456",
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5),
            verified = false,
        )
        `when`(
            verificationRepository.findTopByIdentifierAndPurposeOrderByCreatedAtDesc(
                email,
                VerificationPurpose.RESET_PASSWORD,
            ),
        ).thenReturn(verification)

        val exception =
            assertThrows<BusinessException> { authService.completeResetPassword(email, "newPassword1!") }
        assert(exception.resultCode == ResultCode.RESET_PASSWORD_NOT_VERIFIED)
    }

    @Test
    fun `인증이 완료된 상태로 비밀번호 재설정 완료를 요청하면 새 비밀번호로 변경된다`() {
        val email = "user@example.com"
        val verification = org.kjs.stocknews.model.table.Verification(
            identifier = email,
            purpose = VerificationPurpose.RESET_PASSWORD,
            code = "123456",
            expiresAt = java.time.LocalDateTime.now().plusMinutes(5),
            verified = true,
        )
        val user = User(email = email, password = "encoded", recoveryEmail = "recovery@example.com")
        user.temporaryPassword = true
        `when`(
            verificationRepository.findTopByIdentifierAndPurposeOrderByCreatedAtDesc(
                email,
                VerificationPurpose.RESET_PASSWORD,
            ),
        ).thenReturn(verification)
        `when`(userRepository.findByEmail(email)).thenReturn(user)
        `when`(passwordEncoder.encode(org.mockito.ArgumentMatchers.anyString())).thenReturn("new-encoded")

        authService.completeResetPassword(email, "newPassword1!")

        assert(user.password == "new-encoded")
        assert(!user.temporaryPassword)
        org.mockito.Mockito.verify(verificationRepository).deleteByIdentifierAndPurpose(email, VerificationPurpose.RESET_PASSWORD)
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
        `when`(
            verificationRepository.findTopByIdentifierAndPurposeOrderByCreatedAtDesc(
                email,
                VerificationPurpose.RESET_PASSWORD,
            ),
        ).thenReturn(verification)

        val exception =
            assertThrows<BusinessException> { authService.confirmResetPassword(email, "999999") }
        assert(exception.resultCode == ResultCode.VERIFICATION_CODE_MISMATCH)
    }

    @Test
    fun `임시 비밀번호로 로그인하면 requiresPasswordChange가 true로 반환된다`() {
        val user = mock(User::class.java)
        `when`(user.id).thenReturn(1L)
        `when`(user.email).thenReturn("user@example.com")
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
        `when`(user.email).thenReturn("user@example.com")
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
