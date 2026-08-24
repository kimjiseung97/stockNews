package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.MailSendSettingResponse
import org.kjs.stocknews.model.table.UserMailSendSetting
import org.kjs.stocknews.repository.UserMailSendSettingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val DEFAULT_MAIL_ENABLED = true

// 설정 row가 없는 유저(회원가입 시점에는 만들지 않음)는 기본 발송 대상으로 취급한다.
@Service
class UserMailSendSettingService(
    private val userMailSendSettingRepository: UserMailSendSettingRepository,
) {
    fun get(userId: Long): MailSendSettingResponse =
        MailSendSettingResponse(userMailSendSettingRepository.findByUserId(userId)?.mailEnabled ?: DEFAULT_MAIL_ENABLED)

    @Transactional
    fun register(userId: Long, mailEnabled: Boolean): MailSendSettingResponse {
        val setting = userMailSendSettingRepository.findByUserId(userId)
        if (setting != null) {
            setting.mailEnabled = mailEnabled
        } else {
            userMailSendSettingRepository.save(UserMailSendSetting(userId = userId, mailEnabled = mailEnabled))
        }
        return MailSendSettingResponse(mailEnabled)
    }
}
