package org.kjs.stocknews.service

import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.MailDispatchSettingResponse
import org.kjs.stocknews.model.table.UserMailDispatchSetting
import org.kjs.stocknews.repository.UserMailDispatchSettingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

private val DEFAULT_DISPATCH_TIME: LocalTime = LocalTime.of(9, 0)
private const val DISPATCH_SLOT_MINUTES = 30

// 설정 row가 없는 유저(회원가입 시점에는 만들지 않음)는 기본 발송시간대(09:00)로 취급한다.
@Service
class UserMailDispatchSettingService(
    private val userMailDispatchSettingRepository: UserMailDispatchSettingRepository,
) {
    fun get(userId: Long): MailDispatchSettingResponse =
        MailDispatchSettingResponse(userMailDispatchSettingRepository.findByUserId(userId)?.dispatchTime ?: DEFAULT_DISPATCH_TIME)

    @Transactional
    fun register(userId: Long, dispatchTime: LocalTime): MailDispatchSettingResponse {
        validateDispatchTime(dispatchTime)

        val setting = userMailDispatchSettingRepository.findByUserId(userId)
        if (setting != null) {
            setting.dispatchTime = dispatchTime
        } else {
            userMailDispatchSettingRepository.save(UserMailDispatchSetting(userId = userId, dispatchTime = dispatchTime))
        }
        return MailDispatchSettingResponse(dispatchTime)
    }

    private fun validateDispatchTime(dispatchTime: LocalTime) {
        val isAligned = dispatchTime.minute % DISPATCH_SLOT_MINUTES == 0 && dispatchTime.second == 0 && dispatchTime.nano == 0
        if (!isAligned) throw BusinessException(ResultCode.INVALID_MAIL_DISPATCH_TIME)
    }
}
