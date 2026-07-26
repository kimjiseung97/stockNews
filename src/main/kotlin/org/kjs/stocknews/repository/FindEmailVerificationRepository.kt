package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.FindEmailVerification
import org.springframework.data.jpa.repository.JpaRepository

interface FindEmailVerificationRepository : JpaRepository<FindEmailVerification, String>
