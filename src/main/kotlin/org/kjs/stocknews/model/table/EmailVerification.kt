package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

// EMAIL은 더 이상 PK가 아님 — 10분 내 재발송 횟수를 세려면 발송마다 새 row가 쌓여야 하므로
// auto-increment PK + CREATED_AT으로 이력을 남기는 구조로 변경.
@Entity
@Table(name = "TB_EMAIL_VERIFICATION")
class EmailVerification(
    @Column(name = "EMAIL", nullable = false, length = 254)
    val email: String,

    @Column(name = "CODE", nullable = false, length = 10)
    var code: String,

    @Column(name = "EXPIRES_AT", nullable = false)
    var expiresAt: LocalDateTime,

    @Column(name = "VERIFIED", nullable = false)
    var verified: Boolean = false,

    @Column(name = "CREATED_AT", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "ATTEMPT_COUNT", nullable = false)
    var attemptCount: Int = 0,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Long? = null
}
