package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "TB_EMAIL_VERIFICATION")
class EmailVerification(
    @Id
    @Column(name = "EMAIL", nullable = false, length = 254)
    val email: String,

    @Column(name = "PASSWORD", nullable = false, length = 100)
    var password: String,

    @Column(name = "CODE", nullable = false, length = 10)
    var code: String,

    @Column(name = "EXPIRES_AT", nullable = false)
    var expiresAt: LocalDateTime,
)
