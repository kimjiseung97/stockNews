package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "TB_FIND_EMAIL_VERIFICATION")
class FindEmailVerification(
    @Id
    @Column(name = "RECOVERY_EMAIL", nullable = false, length = 254)
    val recoveryEmail: String,

    @Column(name = "CODE", nullable = false, length = 10)
    var code: String,

    @Column(name = "EXPIRES_AT", nullable = false)
    var expiresAt: LocalDateTime,
)
