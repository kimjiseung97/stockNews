package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalTime

@Entity
@Table(
    name = "TB_USER_MAIL_DISPATCH_SETTING",
    uniqueConstraints = [UniqueConstraint(name = "UK_TB_USER_MAIL_DISPATCH_SETTING_USER", columnNames = ["USER_ID"])],
)
class UserMailDispatchSetting(
    @Column(name = "USER_ID", nullable = false)
    val userId: Long,

    @Column(name = "DISPATCH_TIME", nullable = false)
    var dispatchTime: LocalTime,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Long? = null
        protected set
}
