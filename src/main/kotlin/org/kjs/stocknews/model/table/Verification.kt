package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

// (IDENTIFIER, PURPOSE) 유니크 제약 제거 — 10분 내 재요청 횟수를 세려면 요청마다 새 row가
// 쌓여야 하므로 identifier+purpose당 단일 row를 덮어쓰지 않는다.
@Entity
@Table(name = "TB_VERIFICATION")
class Verification(
    @Column(name = "IDENTIFIER", nullable = false, length = 254)
    val identifier: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "PURPOSE", nullable = false, length = 30)
    val purpose: VerificationPurpose,

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
        protected set
}
