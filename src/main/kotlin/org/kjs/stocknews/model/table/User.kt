package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "TB_USER")
class User(
    @Column(name = "EMAIL", nullable = false, unique = true, length = 254)
    val email: String,

    @Column(name = "PASSWORD", nullable = false, length = 100)
    var password: String,

    @Column(name = "RECOVERY_EMAIL", nullable = true, unique = true, length = 254)
    var recoveryEmail: String? = null,

    @Column(name = "ACTIVE", nullable = false)
    var active: Boolean = true,

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Long? = null
        protected set
}
