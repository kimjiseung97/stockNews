package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.UserMailSendSetting
import org.springframework.data.jpa.repository.JpaRepository

interface UserMailSendSettingRepository : JpaRepository<UserMailSendSetting, Long>, UserMailSendSettingRepositoryCustom {
    fun findByUserId(userId: Long): UserMailSendSetting?
}
