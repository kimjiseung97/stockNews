package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.EmailVerification
import org.springframework.data.jpa.repository.JpaRepository

interface EmailVerificationRepository : JpaRepository<EmailVerification, String>
