package org.kjs.stocknews.repository

import org.kjs.stocknews.model.dto.EligibleMailUserView
import java.time.LocalTime

interface UserMailSendSettingRepositoryCustom {
    // 발송시간대는 30분 단위(09:00, 09:30, ...)로만 입력받으므로 배치도 정확히 그 시각에 맞춰 30분마다 돈다.
    // 그래서 구간 비교가 아니라 정확히 일치하는 유저만 골라내면 된다.
    fun findEligibleUsersByDispatchTime(dispatchTime: LocalTime): List<EligibleMailUserView>
}
