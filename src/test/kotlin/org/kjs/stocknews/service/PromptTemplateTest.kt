package org.kjs.stocknews.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PromptTemplateTest {
    @Test
    fun `변수를 값으로 치환한다`() {
        val rendered = PromptTemplate.render("오늘은 {{today}}이다.", mapOf("today" to "2026년 9월 13일"))

        assertEquals("오늘은 2026년 9월 13일이다.", rendered)
    }

    @Test
    fun `값이 있으면 조건부 구간이 남는다`() {
        val rendered = PromptTemplate.render(
            "안내.{{#newsContext}} 뉴스: {{newsContext}}{{/newsContext}}",
            mapOf("newsContext" to "- 애플 신제품 발표"),
        )

        assertEquals("안내. 뉴스: - 애플 신제품 발표", rendered)
    }

    @Test
    fun `값이 없거나 공백이면 조건부 구간이 통째로 빠진다`() {
        val template = "안내.{{#newsContext}} 뉴스: {{newsContext}}{{/newsContext}}"

        assertEquals("안내.", PromptTemplate.render(template, mapOf("newsContext" to null)))
        assertEquals("안내.", PromptTemplate.render(template, mapOf("newsContext" to "   ")))
        assertEquals("안내.", PromptTemplate.render(template, emptyMap()))
    }

    @Test
    fun `치환 값에 달러 기호나 중괄호가 있어도 그대로 들어간다`() {
        val rendered = PromptTemplate.render("가격: {{price}}", mapOf("price" to "\$100 {{x}}"))

        assertEquals("가격: \$100 {{x}}", rendered)
    }

    @Test
    fun `정의되지 않은 변수는 지우지 않고 원문을 남긴다`() {
        val rendered = PromptTemplate.render("오타 {{typo}} 확인", mapOf("today" to "값"))

        assertEquals("오타 {{typo}} 확인", rendered)
    }
}
