package org.kjs.stocknews.service

import org.kjs.stocknews.common.CustomException
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
        if (userRepository.existsByEmail(email)) {
            throw CustomException(ResultCode.EMAIL_ALREADY_REGISTERED)
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
        val verification = emailVerificationRepository.findById(email)
            .orElseThrow { CustomException(ResultCode.VERIFICATION_NOT_FOUND) }

        if (verification.expiresAt.isBefore(LocalDateTime.now())) {
            emailVerificationRepository.delete(verification)
            throw CustomException(ResultCode.VERIFICATION_EXPIRED)
        }
        if (verification.code != code) {
            throw CustomException(ResultCode.VERIFICATION_CODE_MISMATCH)
        }

        userRepository.save(User(email = verification.email, password = verification.password))
        emailVerificationRepository.delete(verification)
    }

    fun login(email: String, rawPassword: String): Long {
        val user = userRepository.findByEmail(email) ?: throw CustomException(ResultCode.INVALID_CREDENTIALS)
        if (!passwordEncoder.matches(rawPassword, user.password)) {
            throw CustomException(ResultCode.INVALID_CREDENTIALS)
        }
        return user.id!!
    }

    private fun generateCode(): String =
        (1..codeLength).map { Random.nextInt(0, 10) }.joinToString("")
}
