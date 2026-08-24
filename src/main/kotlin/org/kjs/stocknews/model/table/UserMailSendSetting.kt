package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "TB_USER_MAILSEND_SETTING",
    uniqueConstraints = [UniqueConstraint(name = "UK_TB_USER_MAILSEND_SETTING_USER", columnNames = ["USER_ID"])],
)
class UserMailSendSetting(
    @Column(name = "USER_ID", nullable = false)
    val userId: Long,

    @Column(name = "MAIL_ENABLED", nullable = false)
    var mailEnabled: Boolean = true,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Long? = null
        protected set
}
