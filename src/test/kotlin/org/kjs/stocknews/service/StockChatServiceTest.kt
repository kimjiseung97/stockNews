package org.kjs.stocknews.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.StockChatRequest
import org.kjs.stocknews.model.table.Stock
import org.kjs.stocknews.model.table.StockNews
import org.kjs.stocknews.repository.StockNewsRepository
import org.kjs.stocknews.repository.StockRepository
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

private fun <T> anyArg(): T = ArgumentMatchers.any()
private fun <T> eqArg(value: T): T = ArgumentMatchers.eq(value)

class StockChatServiceTest {
    private val nvidiaChatClient = mock(NvidiaChatClient::class.java)
    private val stockRepository = mock(StockRepository::class.java)
    private val stockNewsRepository = mock(StockNewsRepository::class.java)
    private val stockChatService = StockChatService(nvidiaChatClient, stockRepository, stockNewsRepository)

    @Test
    fun `종목이 언급되지 않은 질문이면 컨텍스트 없이 AI 답변을 반환한다`() {
        `when`(stockRepository.findFirstMentionedInText(anyArg())).thenReturn(null)
        `when`(nvidiaChatClient.chatToLLm(anyArg(), eqArg("최근 실적 어때?")))
            .thenReturn("애플의 최근 실적은...")

        val response = stockChatService.ask(StockChatRequest("최근 실적 어때?"))

        assert(response.answer == "애플의 최근 실적은...")
    }

    @Test
    fun `질문에 종목이 언급되면 그 종목의 최신 뉴스를 컨텍스트로 함께 넘긴다`() {
        val apple = Stock(ticker = "AAPL", name = "Apple Inc", koreanName = "애플").setTestId(1L)
        `when`(stockRepository.findFirstMentionedInText("애플 최근 실적 어때?")).thenReturn(apple)
        `when`(stockNewsRepository.findByStockIdOrderByCollectedAtDesc(1L, PageRequest.of(0, 3)))
            .thenReturn(PageImpl(listOf(StockNews(stockId = 1L, title = "애플 신제품 발표", content = null, url = "https://example.com/1"))))
        var capturedSystemPrompt: String? = null
        `when`(nvidiaChatClient.chatToLLm(anyArg(), eqArg("애플 최근 실적 어때?"))).thenAnswer { invocation ->
            capturedSystemPrompt = invocation.arguments[0] as String
            "애플의 최근 실적은..."
        }

        stockChatService.ask(StockChatRequest("애플 최근 실적 어때?"))

        assert(capturedSystemPrompt?.contains("애플 신제품 발표") == true)
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
        `when`(stockRepository.findFirstMentionedInText(anyArg())).thenReturn(null)
        `when`(nvidiaChatClient.chatToLLm(anyArg(), anyArg())).thenReturn(null)

        val exception = assertThrows<BusinessException> { stockChatService.ask(StockChatRequest("질문")) }
        assert(exception.resultCode == ResultCode.STOCK_CHAT_FAILED)
    }

    private fun Stock.setTestId(id: Long): Stock {
        val field = Stock::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(this, id)
        return this
    }
}
