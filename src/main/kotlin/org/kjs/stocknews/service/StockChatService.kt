package org.kjs.stocknews.service

import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.StockChatResponse
import org.kjs.stocknews.repository.StockNewsRepository
import org.kjs.stocknews.repository.StockRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

private const val QUESTION_MAX_LENGTH = 300
private const val CONTEXT_NEWS_COUNT = 3

private const val SYSTEM_PROMPT_TEMPLATE = """
너는 주식 정보 서비스의 종목 분석 어시스턴트다. 아래 종목 정보와 최근 뉴스를 참고해 사용자 질문에 한국어로 답하라.
종목 정보에 없는 최신 시세, 재무 수치는 모른다고 답하고 추측해서 지어내지 마라. 투자 조언이 아니라 정보 제공 목적임을 답변에 자연스럽게 반영하라.

[종목 정보]
%s

[최근 수집된 뉴스]
%s
"""

// 종목 PK + 사용자 질문을 받아 종목 정보/최근 뉴스를 컨텍스트로 NVIDIA NIM LLM에 질의하고 답변을 반환한다.
@Service
class StockChatService(
    private val stockRepository: StockRepository,
    private val stockNewsRepository: StockNewsRepository,
    private val nvidiaChatClient: NvidiaChatClient,
) {
    fun ask(stockId: Long, question: String): StockChatResponse {
        validateQuestion(question)

        val stock = stockRepository.findById(stockId)
            .orElseThrow { BusinessException(ResultCode.STOCK_NOT_FOUND) }

        val recentNews = stockNewsRepository
            .findByStockIdOrderByCollectedAtDesc(stockId, PageRequest.of(0, CONTEXT_NEWS_COUNT))
            .content

        val stockInfo = "티커: ${stock.ticker}, 종목명: ${stock.name}" +
            (stock.koreanName?.let { ", 한글명: $it" } ?: "") +
            (stock.theme?.let { ", 테마: $it" } ?: "")
        val newsContext = if (recentNews.isEmpty()) {
            "최근 수집된 뉴스가 없음"
        } else {
            recentNews.joinToString("\n") { "- ${it.title}" }
        }

        val systemPrompt = SYSTEM_PROMPT_TEMPLATE.format(stockInfo, newsContext)
        val answer = nvidiaChatClient.chatToLLm(systemPrompt, question)
            ?: throw BusinessException(ResultCode.STOCK_CHAT_FAILED)

        return StockChatResponse(answer)
    }

    private fun validateQuestion(question: String) {
        if (question.isBlank()) throw BusinessException(ResultCode.STOCK_CHAT_QUESTION_REQUIRED)
        if (question.length > QUESTION_MAX_LENGTH) throw BusinessException(ResultCode.STOCK_CHAT_QUESTION_TOO_LONG)
    }
}
