package org.kjs.stocknews.service

import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.table.EmailVerification
import org.kjs.stocknews.model.table.User
import org.kjs.stocknews.repository.EmailVerificationRepository
import org.kjs.stocknews.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val emailVerificationRepository: EmailVerificationRepository,
    private val passwordEncoder: PasswordEncoder,
    private val verificationMailSender: VerificationMailSender,
    @Value("\${auth.verification.code-length}") private val codeLength: Int,
    @Value("\${auth.verification.expiry-minutes}") private val expiryMinutes: Long,
) {
    @Transactional
    fun signUp(email: String, rawPassword: String) {
        validateEmail(email)
        validatePassword(rawPassword)
        if (userRepository.existsByEmail(email)) {
            throw BusinessException(ResultCode.EMAIL_ALREADY_REGISTERED)
        }

        val code = generateCode()
        val verification = EmailVerification(
            email = email,
            password = passwordEncoder.encode(rawPassword)!!,
            code = code,
            expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes),
        )
        emailVerificationRepository.save(verification)
        verificationMailSender.sendVerificationCode(email, code, expiryMinutes)
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

        userRepository.save(User(email = verification.email, password = verification.password))
        emailVerificationRepository.delete(verification)
    }

    fun login(email: String, rawPassword: String): Long {
        validateEmail(email)
        validatePassword(rawPassword)
        val user = userRepository.findByEmail(email) ?: throw BusinessException(ResultCode.INVALID_CREDENTIALS)
        if (!passwordEncoder.matches(rawPassword, user.password)) {
            throw BusinessException(ResultCode.INVALID_CREDENTIALS)
        }
        return user.id!!
    }

    private fun validateEmail(email: String) {
        if (email.isBlank()) throw BusinessException(ResultCode.EMAIL_REQUIRED)
        if (!EMAIL_REGEX.matches(email)) throw BusinessException(ResultCode.INVALID_EMAIL_FORMAT)
        if (email.length > 50) throw BusinessException(ResultCode.EMAIL_TOO_LONG)
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
