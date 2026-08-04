package org.kjs.stocknews.service

import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
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
    fun checkEmailDuplicate(email: String): Boolean {
        validateEmail(email)
        return userRepository.existsByEmail(email)
    }

    @Transactional
    fun signUp(email: String, rawPassword: String, recoveryEmail: String): Boolean {
        validateEmail(email)
        validatePassword(rawPassword)
        validateRecoveryEmail(recoveryEmail)
        if (recoveryEmail == email) {
            throw BusinessException(ResultCode.RECOVERY_EMAIL_SAME_AS_EMAIL)
        }
        if (userRepository.existsByEmail(email)) {
            throw BusinessException(ResultCode.EMAIL_ALREADY_REGISTERED)
        }
        if (userRepository.existsByRecoveryEmail(recoveryEmail)) {
            throw BusinessException(ResultCode.RECOVERY_EMAIL_ALREADY_REGISTERED)
        }

        val code = generateCode()
        val verification = EmailVerification(
            email = email,
            password = passwordEncoder.encode(rawPassword)!!,
            recoveryEmail = recoveryEmail,
            code = code,
            expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes),
        )
        emailVerificationRepository.save(verification)
        return try {
            verificationMailSender.sendVerificationCode(email, code, expiryMinutes)
            true
        } catch (e: MailException) {
            false
        }
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

        userRepository.save(
            User(
                email = verification.email,
                password = verification.password,
                recoveryEmail = verification.recoveryEmail,
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

        val user = userRepository.findByEmail(email) ?: throw BusinessException(ResultCode.EMAIL_NOT_FOUND)
        val recoveryEmail = user.recoveryEmail ?: throw BusinessException(ResultCode.RECOVERY_EMAIL_NOT_FOUND)

        val temporaryPassword = generateTemporaryPassword()
        user.password = passwordEncoder.encode(temporaryPassword)!!
        user.temporaryPassword = true
        userRepository.save(user)
        verificationRepository.delete(verification)
        verificationMailSender.sendTemporaryPassword(recoveryEmail, temporaryPassword)
    }

    fun login(email: String, rawPassword: String): LoginResult {
        validateEmail(email)
        validatePassword(rawPassword)
        val user = userRepository.findByEmail(email) ?: throw BusinessException(ResultCode.INVALID_CREDENTIALS)
        if (!passwordEncoder.matches(rawPassword, user.password)) {
            throw BusinessException(ResultCode.INVALID_CREDENTIALS)
        }
        return LoginResult(userId = user.id!!, requiresPasswordChange = user.temporaryPassword)
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
        userRepository.save(user)
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

    private fun generateTemporaryPassword(): String =
        (1..TEMPORARY_PASSWORD_LENGTH).map { TEMPORARY_PASSWORD_CHARS.random() }.joinToString("")

    companion object {
        private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        private const val TEMPORARY_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%"
        private const val TEMPORARY_PASSWORD_LENGTH = 12
    }
}
