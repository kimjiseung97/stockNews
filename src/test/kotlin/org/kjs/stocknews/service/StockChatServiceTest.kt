package org.kjs.stocknews.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.table.Stock
import org.kjs.stocknews.model.table.StockNews
import org.kjs.stocknews.repository.StockNewsRepository
import org.kjs.stocknews.repository.StockRepository
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

private fun <T> anyArg(): T = ArgumentMatchers.any()
private fun <T> eqArg(value: T): T = ArgumentMatchers.eq(value)

class StockChatServiceTest {
    private val stockRepository = mock(StockRepository::class.java)
    private val stockNewsRepository = mock(StockNewsRepository::class.java)
    private val nvidiaChatClient = mock(NvidiaChatClient::class.java)
    private val stockChatService = StockChatService(stockRepository, stockNewsRepository, nvidiaChatClient)

    @Test
    fun `존재하는 종목과 정상 질문이면 AI 답변을 반환한다`() {
        val stock = Stock(ticker = "AAPL", name = "Apple Inc").setTestId(1L)
        `when`(stockRepository.findById(1L)).thenReturn(Optional.of(stock))
        `when`(stockNewsRepository.findByStockIdOrderByCollectedAtDesc(1L, PageRequest.of(0, 3)))
            .thenReturn(PageImpl(emptyList<StockNews>()))
        `when`(nvidiaChatClient.chatToLLm(anyArg(), eqArg("최근 실적 어때?")))
            .thenReturn("애플의 최근 실적은...")

        val response = stockChatService.ask(1L, "최근 실적 어때?")

        assert(response.answer == "애플의 최근 실적은...")
    }

    @Test
    fun `빈 질문이면 STOCK_CHAT_QUESTION_REQUIRED 예외가 발생한다`() {
        val exception = assertThrows<BusinessException> { stockChatService.ask(1L, "  ") }
        assert(exception.resultCode == ResultCode.STOCK_CHAT_QUESTION_REQUIRED)
    }

    @Test
    fun `질문이 300자를 초과하면 STOCK_CHAT_QUESTION_TOO_LONG 예외가 발생한다`() {
        val longQuestion = "a".repeat(301)
        val exception = assertThrows<BusinessException> { stockChatService.ask(1L, longQuestion) }
        assert(exception.resultCode == ResultCode.STOCK_CHAT_QUESTION_TOO_LONG)
    }

    @Test
    fun `존재하지 않는 종목이면 STOCK_NOT_FOUND 예외가 발생한다`() {
        `when`(stockRepository.findById(1L)).thenReturn(Optional.empty())

        val exception = assertThrows<BusinessException> { stockChatService.ask(1L, "질문") }
        assert(exception.resultCode == ResultCode.STOCK_NOT_FOUND)
    }

    @Test
    fun `AI 클라이언트가 null을 반환하면 STOCK_CHAT_FAILED 예외가 발생한다`() {
        val stock = Stock(ticker = "AAPL", name = "Apple Inc").setTestId(1L)
        `when`(stockRepository.findById(1L)).thenReturn(Optional.of(stock))
        `when`(stockNewsRepository.findByStockIdOrderByCollectedAtDesc(1L, PageRequest.of(0, 3)))
            .thenReturn(PageImpl(emptyList<StockNews>()))
        `when`(nvidiaChatClient.chatToLLm(anyArg(), anyArg())).thenReturn(null)

        val exception = assertThrows<BusinessException> { stockChatService.ask(1L, "질문") }
        assert(exception.resultCode == ResultCode.STOCK_CHAT_FAILED)
    }

    private fun Stock.setTestId(id: Long): Stock {
        val field = Stock::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(this, id)
        return this
    }
}
