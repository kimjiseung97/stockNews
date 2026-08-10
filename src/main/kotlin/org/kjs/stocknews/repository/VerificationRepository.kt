package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.Verification
import org.kjs.stocknews.model.table.VerificationPurpose
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface VerificationRepository : JpaRepository<Verification, Long> {
    fun findByIdentifierAndPurpose(identifier: String, purpose: VerificationPurpose): Verification?
    fun deleteByIdentifierAndPurpose(identifier: String, purpose: VerificationPurpose)
    fun deleteByExpiresAtBefore(expiresAt: LocalDateTime): Long
}
