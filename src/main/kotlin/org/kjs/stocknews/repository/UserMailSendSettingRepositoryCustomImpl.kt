package org.kjs.stocknews.repository

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.kjs.stocknews.model.dto.EligibleMailUserView
import org.kjs.stocknews.model.table.QUser.user
import org.kjs.stocknews.model.table.QUserMailDispatchSetting.userMailDispatchSetting
import org.kjs.stocknews.model.table.QUserMailSendSetting.userMailSendSetting
import org.springframework.stereotype.Repository
import java.time.LocalTime

@Repository
class UserMailSendSettingRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : UserMailSendSettingRepositoryCustom {
    // 발송여부/발송시간대 설정과 유저를 join해, 활성 유저 중 발송여부가 true이고
    // 발송시간대가 이번 배치 실행 시각과 정확히 일치하는 유저만 한 번에 골라낸다.
    // 설정 row가 하나라도 없는 유저는 결과에서 빠진다.
    override fun findEligibleUsersByDispatchTime(dispatchTime: LocalTime): List<EligibleMailUserView> =
        queryFactory
            .select(
                Projections.constructor(
                    EligibleMailUserView::class.java,
                    user.id,
                    user.email,
                    userMailDispatchSetting.dispatchTime,
                ),
            )
            .from(userMailSendSetting)
            .join(userMailDispatchSetting).on(userMailSendSetting.userId.eq(userMailDispatchSetting.userId))
            .join(user).on(userMailSendSetting.userId.eq(user.id))
            .where(
                userMailSendSetting.mailEnabled.isTrue,
                user.active.isTrue,
                userMailDispatchSetting.dispatchTime.eq(dispatchTime),
            )
            .fetch()
}
