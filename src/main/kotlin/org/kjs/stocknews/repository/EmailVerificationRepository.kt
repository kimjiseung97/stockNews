package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.EmailVerification
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface EmailVerificationRepository : JpaRepository<EmailVerification, Long> {
    fun deleteByExpiresAtBefore(expiresAt: LocalDateTime): Long
    fun deleteByEmail(email: String): Long
    fun countByEmailAndCreatedAtAfter(email: String, createdAt: LocalDateTime): Long
    fun findTopByEmailOrderByCreatedAtDesc(email: String): EmailVerification?
}
