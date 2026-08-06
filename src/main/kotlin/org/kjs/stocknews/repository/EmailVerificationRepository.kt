package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.EmailVerification
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface EmailVerificationRepository : JpaRepository<EmailVerification, String> {
    fun deleteByExpiresAtBefore(expiresAt: LocalDateTime): Long
}
