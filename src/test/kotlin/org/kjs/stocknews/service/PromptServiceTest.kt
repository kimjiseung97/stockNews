package org.kjs.stocknews.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.kjs.stocknews.model.table.Prompt
import org.kjs.stocknews.repository.PromptRepository
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.dao.QueryTimeoutException

class PromptServiceTest {
    private val promptRepository = mock(PromptRepository::class.java)
    private val promptService = PromptService(promptRepository, cacheTtlSeconds = 60)

    private fun prompt(content: String) = Prompt(
        code = "DEFAULT_PROMPT",
        name = "종목 챗봇 시스템 프롬프트",
        content = content,
    )

    @Test
    fun `DB에 등록된 프롬프트를 가져와 변수를 채운다`() {
        `when`(promptRepository.findByCodeAndEnabledIsTrue("DEFAULT_PROMPT"))
            .thenReturn(prompt("오늘은 {{today}}이다."))

        val rendered = promptService.render(PromptCode.STOCK_CHAT_SYSTEM, mapOf("today" to "2026년 9월 13일"))

        assertEquals("오늘은 2026년 9월 13일이다.", rendered)
    }

    @Test
    fun `TTL 안에서는 DB를 다시 조회하지 않는다`() {
        `when`(promptRepository.findByCodeAndEnabledIsTrue("DEFAULT_PROMPT"))
            .thenReturn(prompt("내용"))

        repeat(3) { promptService.render(PromptCode.STOCK_CHAT_SYSTEM, emptyMap()) }

        verify(promptRepository, times(1)).findByCodeAndEnabledIsTrue("DEFAULT_PROMPT")
    }

    @Test
    fun `캐시를 비우면 DB를 다시 조회한다`() {
        `when`(promptRepository.findByCodeAndEnabledIsTrue("DEFAULT_PROMPT"))
            .thenReturn(prompt("내용"))

        promptService.render(PromptCode.STOCK_CHAT_SYSTEM, emptyMap())
        promptService.evictCache()
        promptService.render(PromptCode.STOCK_CHAT_SYSTEM, emptyMap())

        verify(promptRepository, times(2)).findByCodeAndEnabledIsTrue("DEFAULT_PROMPT")
    }

    @Test
    fun `등록된 프롬프트가 없으면 코드 기본 프롬프트를 쓴다`() {
        `when`(promptRepository.findByCodeAndEnabledIsTrue("DEFAULT_PROMPT")).thenReturn(null)

        val rendered = promptService.render(PromptCode.STOCK_CHAT_SYSTEM, mapOf("today" to "2026년 9월 13일"))

        assertTrue(rendered.contains("너는 주식 정보 서비스의 어시스턴트다"))
        assertTrue(rendered.contains("2026년 9월 13일"))
    }

    @Test
    fun `캐시 만료 후 DB 조회가 실패하면 만료된 캐시 내용으로 버틴다`() {
        // TTL을 음수로 줘 캐시가 담기자마자 만료되는 상황(=매번 DB를 다시 보러 가는 상황)을 만든다.
        val noCacheService = PromptService(promptRepository, cacheTtlSeconds = -1)
        `when`(promptRepository.findByCodeAndEnabledIsTrue("DEFAULT_PROMPT"))
            .thenReturn(prompt("정상 프롬프트"))
            .thenThrow(QueryTimeoutException("db down"))

        assertEquals("정상 프롬프트", noCacheService.render(PromptCode.STOCK_CHAT_SYSTEM, emptyMap()))
        assertEquals("정상 프롬프트", noCacheService.render(PromptCode.STOCK_CHAT_SYSTEM, emptyMap()))
    }

    @Test
    fun `어드민이 프롬프트를 꺼버리면 캐시를 재사용하지 않고 기본 프롬프트로 내려간다`() {
        val noCacheService = PromptService(promptRepository, cacheTtlSeconds = -1)
        `when`(promptRepository.findByCodeAndEnabledIsTrue("DEFAULT_PROMPT"))
            .thenReturn(prompt("문제가 생긴 프롬프트"))
            .thenReturn(null) // 어드민이 ENABLED=false로 끄거나 삭제한 상황

        assertEquals("문제가 생긴 프롬프트", noCacheService.render(PromptCode.STOCK_CHAT_SYSTEM, emptyMap()))
        val rendered = noCacheService.render(PromptCode.STOCK_CHAT_SYSTEM, emptyMap())

        assertTrue(rendered.contains("너는 주식 정보 서비스의 어시스턴트다"))
    }

    @Test
    fun `DB 조회가 실패했고 캐시도 없으면 코드 기본 프롬프트를 쓴다`() {
        `when`(promptRepository.findByCodeAndEnabledIsTrue("DEFAULT_PROMPT"))
            .thenThrow(QueryTimeoutException("db down"))

        val rendered = promptService.render(PromptCode.STOCK_CHAT_SYSTEM, emptyMap())

        assertTrue(rendered.contains("너는 주식 정보 서비스의 어시스턴트다"))
    }
}
