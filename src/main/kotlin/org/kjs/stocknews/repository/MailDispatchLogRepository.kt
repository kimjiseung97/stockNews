package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.MailDispatchLog
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.time.LocalTime

interface MailDispatchLogRepository : JpaRepository<MailDispatchLog, Long> {
    // 같은 발송 슬롯에 이미 남긴 기록(유니크 키와 동일한 조건). 재처리 결과로 덮어쓸 때 찾는다.
    fun findByUserIdAndDispatchDateAndDispatchTime(
        userId: Long,
        dispatchDate: LocalDate,
        dispatchTime: LocalTime,
    ): MailDispatchLog?
}
