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
) {
    // 이메일 중복 확인 후 사용 가능하면 곧바로 인증코드를 발송한다.
    @Transactional
    fun checkEmailDuplicate(email: String): EmailAvailabilityResult {
        validateEmail(email)
        if (userRepository.existsByEmail(email)) {
            return EmailAvailabilityResult(duplicated = true, isMailSendSuccess = false)
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

    @Transactional
    fun verifyEmail(email: String, code: String) {
        validateEmail(email)
        validateCode(code)
        val verification = emailVerificationRepository.findById(email)
            .orElseThrow { BusinessException(ResultCode.VERIFICATION_NOT_FOUND) }

        if (verification.expiresAt.isBefore(LocalDateTime.now())) {
            emailVerificationRepository.delete(verification)
            throw BusinessException(ResultCode.VERIFICATION_EXPIRED)
        }
        if (verification.code != code) {
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

        val verification = emailVerificationRepository.findById(email)
            .orElseThrow { BusinessException(ResultCode.VERIFICATION_NOT_FOUND) }
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
        emailVerificationRepository.delete(verification)
    }

    @Transactional
    fun requestFindEmail(recoveryEmail: String) {
        validateRecoveryEmail(recoveryEmail)
        if (!userRepository.existsByRecoveryEmail(recoveryEmail)) {
            throw BusinessException(ResultCode.RECOVERY_EMAIL_NOT_FOUND)
        }

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
        val verification = verificationRepository.findByIdentifierAndPurpose(recoveryEmail, VerificationPurpose.FIND_EMAIL)
            ?: throw BusinessException(ResultCode.VERIFICATION_NOT_FOUND)

        if (verification.expiresAt.isBefore(LocalDateTime.now())) {
            verificationRepository.delete(verification)
            throw BusinessException(ResultCode.VERIFICATION_EXPIRED)
        }
        if (verification.code != code) {
            throw BusinessException(ResultCode.VERIFICATION_CODE_MISMATCH)
        }

        val user = userRepository.findByRecoveryEmail(recoveryEmail)
            ?: throw BusinessException(ResultCode.RECOVERY_EMAIL_NOT_FOUND)
        verificationRepository.delete(verification)
        return user.email
    }

    @Transactional
    fun requestResetPassword(email: String) {
        validateEmail(email)
        val user = userRepository.findByEmail(email) ?: throw BusinessException(ResultCode.EMAIL_NOT_FOUND)
        val recoveryEmail = user.recoveryEmail ?: throw BusinessException(ResultCode.RECOVERY_EMAIL_NOT_FOUND)

        val code = generateCode()
        val verification = Verification(
            identifier = email,
            purpose = VerificationPurpose.RESET_PASSWORD,
            code = code,
            expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes),
        )
        verificationRepository.save(verification)
        verificationMailSender.sendResetPasswordCode(recoveryEmail, code, expiryMinutes)
    }

    @Transactional
    fun confirmResetPassword(email: String, code: String) {
        validateEmail(email)
        validateCode(code)
        val verification = verificationRepository.findByIdentifierAndPurpose(email, VerificationPurpose.RESET_PASSWORD)
            ?: throw BusinessException(ResultCode.VERIFICATION_NOT_FOUND)

        if (verification.expiresAt.isBefore(LocalDateTime.now())) {
            verificationRepository.delete(verification)
            throw BusinessException(ResultCode.VERIFICATION_EXPIRED)
        }
        if (verification.code != code) {
            throw BusinessException(ResultCode.VERIFICATION_CODE_MISMATCH)
        }

        verification.verified = true
        verification.expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes)
    }

    @Transactional
    fun completeResetPassword(email: String, newPassword: String) {
        validateEmail(email)
        validatePassword(newPassword)
        val verification = verificationRepository.findByIdentifierAndPurpose(email, VerificationPurpose.RESET_PASSWORD)
            ?: throw BusinessException(ResultCode.VERIFICATION_NOT_FOUND)

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
        verificationRepository.delete(verification)
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

    companion object {
        private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
