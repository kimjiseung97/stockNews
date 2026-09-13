package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// 다이제스트 메일 발송 1건의 기록. 쓰기 주체는 이 프로젝트(NewsDispatchJobConfig)이고 어드민(stockNewsAdmin)은 조회만 한다.
// Spring Batch 메타데이터(WRITE_COUNT)로는 유저 단위 집계와 실패 사유 추적이 안 돼 별도 테이블로 둔다.
//
// (USER_ID, DISPATCH_DATE, DISPATCH_TIME) 유니크: 발송 실패로 청크가 롤백되면 Spring Batch가 그 청크를
// 건별로 재처리하는데, 이 로그는 롤백되지 않는 별도 트랜잭션으로 쓰기 때문에 재처리 시 같은 유저 로그가
// 두 번 들어갈 수 있다. 슬롯당 유저 1건으로 DB가 막아준다 - MailDispatchLogService 참고.
@Entity
@Table(
    name = "TB_MAIL_DISPATCH_LOG",
    // 유저별 조회는 아래 유니크 키의 선두 컬럼(USER_ID, DISPATCH_DATE)이 그대로 처리하므로 별도 인덱스를 두지 않는다.
    indexes = [
        Index(name = "IDX_TB_MAIL_DISPATCH_LOG_DATE", columnList = "DISPATCH_DATE"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "UK_TB_MAIL_DISPATCH_LOG_SLOT", columnNames = ["USER_ID", "DISPATCH_DATE", "DISPATCH_TIME"]),
    ],
)
class MailDispatchLog(
    @Column(name = "USER_ID", nullable = false)
    val userId: Long,

    // 발송 시점의 수신 주소 스냅샷 - 유저가 이메일을 바꿔도 과거 로그는 그대로 남게 한다.
    @Column(name = "EMAIL", nullable = false, length = 254)
    val email: String,

    @Column(name = "DISPATCH_DATE", nullable = false)
    val dispatchDate: LocalDate,

    @Column(name = "DISPATCH_TIME", nullable = false)
    val dispatchTime: LocalTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    var status: MailDispatchStatus,

    @Column(name = "STOCK_COUNT", nullable = false)
    var stockCount: Int = 0,

    @Column(name = "ARTICLE_COUNT", nullable = false)
    var articleCount: Int = 0,

    @Column(name = "ERROR_MESSAGE", length = 500)
    var errorMessage: String? = null,

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Long? = null
        protected set

    // 같은 슬롯을 재처리한 결과로 덮어쓴다. 실패 후 재시도가 성공하면 FAILED가 SUCCESS로 바뀌어야 하므로
    // 먼저 쓴 기록이 이기게 두지 않고 마지막 결과를 남긴다.
    fun overwriteWith(status: MailDispatchStatus, stockCount: Int, articleCount: Int, errorMessage: String?) {
        this.status = status
        this.stockCount = stockCount
        this.articleCount = articleCount
        this.errorMessage = errorMessage
    }
}
