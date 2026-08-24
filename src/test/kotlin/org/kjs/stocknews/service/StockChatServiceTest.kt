package org.kjs.stocknews.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.StockChatRequest
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

private fun <T> anyArg(): T = ArgumentMatchers.any()
private fun <T> eqArg(value: T): T = ArgumentMatchers.eq(value)

class StockChatServiceTest {
    private val nvidiaChatClient = mock(NvidiaChatClient::class.java)
    private val stockChatService = StockChatService(nvidiaChatClient)

    @Test
    fun `정상 질문이면 AI 답변을 반환한다`() {
        `when`(nvidiaChatClient.chatToLLm(anyArg(), eqArg("최근 실적 어때?")))
            .thenReturn("애플의 최근 실적은...")

        val response = stockChatService.ask(StockChatRequest("최근 실적 어때?"))

        assert(response.answer == "애플의 최근 실적은...")
    }

    @Test
    fun `빈 질문이면 STOCK_CHAT_QUESTION_REQUIRED 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> { stockChatService.ask(StockChatRequest("  ")) }
        assert(exception.resultCode == ResultCode.STOCK_CHAT_QUESTION_REQUIRED)
    }

    @Test
    fun `질문이 300자를 초과하면 STOCK_CHAT_QUESTION_TOO_LONG 예외가 발생한다`() {
        val longQuestion = "a".repeat(301)
        val exception = assertThrows<BusinessException> { stockChatService.ask(StockChatRequest(longQuestion)) }
        assert(exception.resultCode == ResultCode.STOCK_CHAT_QUESTION_TOO_LONG)
    }

    @Test
    fun `AI 클라이언트가 null을 반환하면 STOCK_CHAT_FAILED 예외가 발생한다`() {
        `when`(nvidiaChatClient.chatToLLm(anyArg(), anyArg())).thenReturn(null)

        val exception = assertThrows<BusinessException> { stockChatService.ask(StockChatRequest("질문")) }
        assert(exception.resultCode == ResultCode.STOCK_CHAT_FAILED)
    }
}
