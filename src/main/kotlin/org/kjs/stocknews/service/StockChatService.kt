package org.kjs.stocknews.service

import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.StockChatRequest
import org.kjs.stocknews.model.dto.StockChatResponse
import org.springframework.stereotype.Service

private const val QUESTION_MAX_LENGTH = 300

private const val SYSTEM_PROMPT = """
너는 주식 정보 서비스의 어시스턴트다. 사용자 질문에 무조건 한국어로 답하라.
모르는 최신 시세, 재무 수치는 모른다고 답하고 추측해서 지어내지 마라. 투자 조언이 아니라 정보 제공 목적임을 답변에 자연스럽게 반영하라.
"""

// 사용자의 평문 질문을 받아 NVIDIA NIM LLM에 질의하고 답변을 반환한다.
@Service
class StockChatService(
    private val nvidiaChatClient: NvidiaChatClient,
) {
    fun ask(request: StockChatRequest): StockChatResponse {
        val question = request.question
        validateQuestion(question)

        val answer = nvidiaChatClient.chatToLLm(SYSTEM_PROMPT, question)
            ?: throw BusinessException(ResultCode.STOCK_CHAT_FAILED)

        return StockChatResponse(answer)
    }

    private fun validateQuestion(question: String) {
        if (question.isBlank()) throw BusinessException(ResultCode.STOCK_CHAT_QUESTION_REQUIRED)
        if (question.length > QUESTION_MAX_LENGTH) throw BusinessException(ResultCode.STOCK_CHAT_QUESTION_TOO_LONG)
    }
}
