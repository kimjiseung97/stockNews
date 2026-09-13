package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.Prompt
import org.springframework.data.jpa.repository.JpaRepository

interface PromptRepository : JpaRepository<Prompt, Long> {
    // 어드민에서 비활성(ENABLED=false)으로 꺼둔 프롬프트는 없는 것으로 취급한다.
    fun findByCodeAndEnabledIsTrue(code: String): Prompt?
}
