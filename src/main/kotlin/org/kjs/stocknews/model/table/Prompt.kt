package org.kjs.stocknews.model.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDateTime

// 어드민(stockNewsAdmin)이 등록/수정하는 LLM 프롬프트 원본의 조회 전용 미러 엔티티.
// 쓰기 주체는 어드민 하나뿐이라 이쪽에서는 절대 수정하지 않는다(@Immutable).
// CODE로 찾아(PromptCode 참고) CONTENT를 템플릿으로 렌더링해 쓴다 - PromptService 참고.
@Entity
@Immutable
@Table(name = "TB_PROMPT")
class Prompt(
    @Column(name = "CODE", nullable = false, unique = true, length = 60)
    val code: String,

    @Column(name = "NAME", nullable = false, length = 100)
    val name: String,

    // 실제 컬럼은 어드민(stockNewsAdmin)이 만든 LONGTEXT다. 예전 @Lob은 MariaDB에서 tinytext로 매핑돼
    // ddl-auto=update가 기동마다 "modify column CONTENT tinytext"를 시도했고, 프롬프트가 255바이트를 넘어
    // 1406(Data too long)으로 거절되는 걸로 버텼다(2026-09-22 운영 로그). 프롬프트가 짧았다면 성공해서
    // 어드민이 긴 프롬프트를 저장할 수 없게 됐을 것이다. 타입을 명시해 DDL이 실제와 일치하게 한다.
    @Column(name = "CONTENT", nullable = false, columnDefinition = "LONGTEXT")
    val content: String,

    @Column(name = "DESCRIPTION", length = 300)
    val description: String? = null,

    @Column(name = "ENABLED", nullable = false)
    val enabled: Boolean = true,

    @Column(name = "VERSION", nullable = false)
    val version: Int = 1,

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "UPDATED_AT", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    var id: Long? = null
        protected set
}
