package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "TB_USER_STOCK",
    uniqueConstraints = [UniqueConstraint(name = "UK_TB_USER_STOCK_USER_STOCK", columnNames = ["USER_ID", "STOCK_ID"])],
)
class UserStock(
    @Column(name = "USER_ID", nullable = false)
    val userId: Long,

    @Column(name = "STOCK_ID", nullable = false)
    val stockId: Long,

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Long? = null
        protected set
}
