package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.UserMailDispatchSetting
import org.springframework.data.jpa.repository.JpaRepository

interface UserMailDispatchSettingRepository : JpaRepository<UserMailDispatchSetting, Long> {
    fun findByUserId(userId: Long): UserMailDispatchSetting?
}
