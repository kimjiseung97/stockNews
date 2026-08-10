package org.kjs.stocknews.service

import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.EmailAvailabilityResult
import org.kjs.stocknews.model.dto.LoginResult
import org.kjs.stocknews.model.table.EmailVerification
import org.kjs.stocknews.model.table.User
import org.kjs.stocknews.model.table.Verification
import org.kjs.stocknews.model.table.VerificationPurpose
import org.kjs.stocknews.repository.EmailVerificationRepository
import org.kjs.stocknews.repository.UserRepository
import org.kjs.stocknews.repository.VerificationRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.MailException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val emailVerificationRepository: EmailVerificationRepository,
    private val verificationRepository: VerificationRepository,
    private val passwordEncoder: PasswordEncoder,
    private val verificationMailSender: VerificationMailSender,
    @Value("\${auth.verification.code-length}") private val codeLength: Int,
    @Value("\${auth.verification.expiry-minutes}") private val expiryMinutes: Long,
    @Value("\${auth.verification.max-requests:3}") private val maxRequestsPerWindow: Long,
    @Value("\${auth.verification.max-attempts:3}") private val maxAttempts: Int,
) {
    // 이메일 중복 확인 후 사용 가능하면 곧바로 인증코드를 발송한다.
    // 동일 이메일이 expiry-minutes(기본 10분) 내에 max-requests(기본 3회) 이상 요청하면 차단한다.
    @Transactional
    fun checkEmailDuplicate(email: String): EmailAvailabilityResult {
        validateEmail(email)
        if (userRepository.existsByEmail(email)) {
            return EmailAvailabilityResult(duplicated = true, isMailSendSuccess = false)
        }

        val recentRequestCount = emailVerificationRepository.countByEmailAndCreatedAtAfter(
            email,
            LocalDateTime.now().minusMinutes(expiryMinutes),
        )
        if (recentRequestCount >= maxRequestsPerWindow) {
            throw BusinessException(ResultCode.VERIFICATION_REQUEST_LIMIT_EXCEEDED)
        }

        val code = generateCode()
        val verification = EmailVerification(
            email = email,
            code = code,
            expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes),
        )
        emailVerificationRepository.save(verification)
        val isMailSendSuccess = try {
            verificationMailSender.sendVerificationCode(email, code, expiryMinutes)
            true
        } catch (e: MailException) {
            false
        }
        return EmailAvailabilityResult(duplicated = false, isMailSendSuccess = isMailSendSuccess)
    }

    // 동일 코드에 대한 오입력이 max-attempts(기본 5회) 이상이면 차단한다 — 새 코드를 요청하면 초기화된다.
    @Transactional
    fun verifyEmail(email: String, code: String) {
        validateEmail(email)
        validateCode(code)
        val verification = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
            ?: throw BusinessException(ResultCode.VERIFICATION_NOT_FOUND)

        if (verification.expiresAt.isBefore(LocalDateTime.now())) {
            emailVerificationRepository.delete(verification)
            throw BusinessException(ResultCode.VERIFICATION_EXPIRED)
        }
        if (verification.attemptCount >= maxAttempts) {
            throw BusinessException(ResultCode.VERIFICATION_ATTEMPTS_EXCEEDED)
        }
        if (verification.code != code) {
            verification.attemptCount += 1
            throw BusinessException(ResultCode.VERIFICATION_CODE_MISMATCH)
        }

        verification.verified = true
        verification.expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes)
    }

    @Transactional
    fun completeSignUp(email: String, rawPassword: String, recoveryEmail: String) {
        validateEmail(email)
        validatePassword(rawPassword)
        validateRecoveryEmail(recoveryEmail)
        if (recoveryEmail == email) {
            throw BusinessException(ResultCode.RECOVERY_EMAIL_SAME_AS_EMAIL)
        }

        val verification = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
            ?: throw BusinessException(ResultCode.VERIFICATION_NOT_FOUND)
        if (verification.expiresAt.isBefore(LocalDateTime.now())) {
            emailVerificationRepository.delete(verification)
            throw BusinessException(ResultCode.VERIFICATION_EXPIRED)
        }
        if (!verification.verified) {
            throw BusinessException(ResultCode.EMAIL_NOT_VERIFIED)
        }
        if (userRepository.existsByEmail(email)) {
            throw BusinessException(ResultCode.EMAIL_ALREADY_REGISTERED)
        }
        if (userRepository.existsByRecoveryEmail(recoveryEmail)) {
            throw BusinessException(ResultCode.RECOVERY_EMAIL_ALREADY_REGISTERED)
        }

        userRepository.save(
            User(
                email = email,
                password = passwordEncoder.encode(rawPassword)!!,
                recoveryEmail = recoveryEmail,
            ),
        )
        emailVerificationRepository.deleteByEmail(email)
    }

    // 동일 identifier+purpose가 expiry-minutes(기본 10분) 내에 max-requests(기본 3회) 이상 요청하면 차단한다.
    @Transactional
    fun requestFindEmail(recoveryEmail: String) {
        validateRecoveryEmail(recoveryEmail)
        if (!userRepository.existsByRecoveryEmail(recoveryEmail)) {
            throw BusinessException(ResultCode.RECOVERY_EMAIL_NOT_FOUND)
        }
        assertWithinRequestLimit(recoveryEmail, VerificationPurpose.FIND_EMAIL)

        val code = generateCode()
        val verification = Verification(
            identifier = recoveryEmail,
            purpose = VerificationPurpose.FIND_EMAIL,
            code = code,
            expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes),
        )
        verificationRepository.save(verification)
        verificationMailSender.sendFindEmailCode(recoveryEmail, code, expiryMinutes)
    }

    @Transactional
    fun verifyFindEmail(recoveryEmail: String, code: String): String {
        validateRecoveryEmail(recoveryEmail)
        validateCode(code)
        val verification = verificationRepository.findTopByIdentifierAndPurposeOrderByCreatedAtDesc(
            recoveryEmail,
            VerificationPurpose.FIND_EMAIL,
        ) ?: throw BusinessException(ResultCode.VERIFICATION_NOT_FOUND)

        if (verification.expiresAt.isBefore(LocalDateTime.now())) {
            verificationRepository.delete(verification)
            throw BusinessException(ResultCode.VERIFICATION_EXPIRED)
        }
        if (verification.attemptCount >= maxAttempts) {
            throw BusinessException(ResultCode.VERIFICATION_ATTEMPTS_EXCEEDED)
        }
        if (verification.code != code) {
            verification.attemptCount += 1
            throw BusinessException(ResultCode.VERIFICATION_CODE_MISMATCH)
        }

        val user = userRepository.findByRecoveryEmail(recoveryEmail)
            ?: throw BusinessException(ResultCode.RECOVERY_EMAIL_NOT_FOUND)
        verificationRepository.deleteByIdentifierAndPurpose(recoveryEmail, VerificationPurpose.FIND_EMAIL)
        return user.email
    }

    // 동일 identifier+purpose가 expiry-minutes(기본 10분) 내에 max-requests(기본 3회) 이상 요청하면 차단한다.
    @Transactional
    fun requestResetPassword(email: String): Boolean {
        validateEmail(email)
        val user = userRepository.findByEmail(email) ?: return false
        val recoveryEmail = user.recoveryEmail ?: throw BusinessException(ResultCode.RECOVERY_EMAIL_NOT_FOUND)
        assertWithinRequestLimit(email, VerificationPurpose.RESET_PASSWORD)

        val code = generateCode()
        val verification = Verification(
            identifier = email,
            purpose = VerificationPurpose.RESET_PASSWORD,
            code = code,
            expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes),
        )
        verificationRepository.save(verification)
        verificationMailSender.sendResetPasswordCode(recoveryEmail, code, expiryMinutes)
        return true
    }

    @Transactional
    fun confirmResetPassword(email: String, code: String) {
        validateEmail(email)
        validateCode(code)
        val verification = verificationRepository.findTopByIdentifierAndPurposeOrderByCreatedAtDesc(
            email,
            VerificationPurpose.RESET_PASSWORD,
        ) ?: throw BusinessException(ResultCode.VERIFICATION_NOT_FOUND)

        if (verification.expiresAt.isBefore(LocalDateTime.now())) {
            verificationRepository.delete(verification)
            throw BusinessException(ResultCode.VERIFICATION_EXPIRED)
        }
        if (verification.attemptCount >= maxAttempts) {
            throw BusinessException(ResultCode.VERIFICATION_ATTEMPTS_EXCEEDED)
        }
        if (verification.code != code) {
            verification.attemptCount += 1
            throw BusinessException(ResultCode.VERIFICATION_CODE_MISMATCH)
        }

        verification.verified = true
        verification.expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes)
    }

    @Transactional
    fun completeResetPassword(email: String, newPassword: String) {
        validateEmail(email)
        validatePassword(newPassword)
        val verification = verificationRepository.findTopByIdentifierAndPurposeOrderByCreatedAtDesc(
            email,
            VerificationPurpose.RESET_PASSWORD,
        ) ?: throw BusinessException(ResultCode.VERIFICATION_NOT_FOUND)

        if (verification.expiresAt.isBefore(LocalDateTime.now())) {
            verificationRepository.delete(verification)
            throw BusinessException(ResultCode.VERIFICATION_EXPIRED)
        }
        if (!verification.verified) {
            throw BusinessException(ResultCode.RESET_PASSWORD_NOT_VERIFIED)
        }

        val user = userRepository.findByEmail(email) ?: throw BusinessException(ResultCode.EMAIL_NOT_FOUND)
        user.password = passwordEncoder.encode(newPassword)!!
        user.temporaryPassword = false
        verificationRepository.deleteByIdentifierAndPurpose(email, VerificationPurpose.RESET_PASSWORD)
    }

    fun login(email: String, rawPassword: String): LoginResult {
        validateEmail(email)
        validatePassword(rawPassword)
        val user = userRepository.findByEmail(email) ?: throw BusinessException(ResultCode.INVALID_CREDENTIALS)
        if (!passwordEncoder.matches(rawPassword, user.password)) {
            throw BusinessException(ResultCode.INVALID_CREDENTIALS)
        }
        return LoginResult(userId = user.id!!, email = user.email, requiresPasswordChange = user.temporaryPassword)
    }

    @Transactional
    fun changePassword(userId: Long, currentPassword: String, newPassword: String) {
        validatePassword(currentPassword)
        validatePassword(newPassword)
        val user = userRepository.findById(userId).orElseThrow { BusinessException(ResultCode.UNAUTHORIZED) }
        if (!passwordEncoder.matches(currentPassword, user.password)) {
            throw BusinessException(ResultCode.CURRENT_PASSWORD_MISMATCH)
        }

        user.password = passwordEncoder.encode(newPassword)!!
        user.temporaryPassword = false
    }

    private fun validateEmail(email: String) {
        if (email.isBlank()) throw BusinessException(ResultCode.EMAIL_REQUIRED)
        if (!EMAIL_REGEX.matches(email)) throw BusinessException(ResultCode.INVALID_EMAIL_FORMAT)
        if (email.length > 50) throw BusinessException(ResultCode.EMAIL_TOO_LONG)
    }

    private fun validateRecoveryEmail(recoveryEmail: String) {
        if (recoveryEmail.isBlank()) throw BusinessException(ResultCode.RECOVERY_EMAIL_REQUIRED)
        if (!EMAIL_REGEX.matches(recoveryEmail)) throw BusinessException(ResultCode.INVALID_RECOVERY_EMAIL_FORMAT)
        if (recoveryEmail.length > 50) throw BusinessException(ResultCode.RECOVERY_EMAIL_TOO_LONG)
    }

    private fun validatePassword(password: String) {
        if (password.isBlank()) throw BusinessException(ResultCode.PASSWORD_REQUIRED)
        if (password.length !in 8..20) throw BusinessException(ResultCode.INVALID_PASSWORD_LENGTH)
    }

    private fun validateCode(code: String) {
        if (code.isBlank()) throw BusinessException(ResultCode.VERIFICATION_CODE_REQUIRED)
        if (code.length != codeLength) throw BusinessException(ResultCode.INVALID_VERIFICATION_CODE_LENGTH)
    }

    private fun generateCode(): String =
        (1..codeLength).map { Random.nextInt(0, 10) }.joinToString("")

    private fun assertWithinRequestLimit(identifier: String, purpose: VerificationPurpose) {
        val recentRequestCount = verificationRepository.countByIdentifierAndPurposeAndCreatedAtAfter(
            identifier,
            purpose,
            LocalDateTime.now().minusMinutes(expiryMinutes),
        )
        if (recentRequestCount >= maxRequestsPerWindow) {
            throw BusinessException(ResultCode.VERIFICATION_REQUEST_LIMIT_EXCEEDED)
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
