package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "TB_VERIFICATION",
    uniqueConstraints = [
        UniqueConstraint(name = "UK_TB_VERIFICATION_IDENTIFIER_PURPOSE", columnNames = ["IDENTIFIER", "PURPOSE"]),
    ],
)
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
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Long? = null
        protected set
}
