package org.kjs.stocknews.service

import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.StockChatRequest
import org.kjs.stocknews.model.dto.StockChatResponse
import org.kjs.stocknews.model.table.Stock
import org.kjs.stocknews.repository.StockNewsRepository
import org.kjs.stocknews.repository.StockRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val QUESTION_MAX_LENGTH = 300
private const val CONTEXT_NEWS_COUNT = 3

private val SYSTEM_PROMPT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy년 M월 d일")

// LLM 학습 시점 지식(예: 2023년 기준)에 갇혀 있으면 "올해"를 잘못 판단해 최신 소식을 과거/미래 취급하므로
// 매 요청마다 실제 오늘 날짜를 프롬프트에 박아 넣어 기준을 맞춰준다.
// 학습 데이터 자체엔 없는 실제 최신 뉴스는 날짜만 알려줘선 못 채워지므로, 질문에 종목이 언급되면
// TB_STOCK_NEWS에서 실제 수집된 최신 뉴스를 찾아 컨텍스트로 같이 넘겨(경량 RAG) 그 데이터를 근거로 답하게 한다.
private fun systemPrompt(newsContext: String?): String {
    val newsSection = if (newsContext == null) {
        ""
    } else {
        """

        아래는 방금 조회한 실제 최신 뉴스다. 네가 학습한 지식보다 이 데이터를 우선해서 답하라.
        $newsContext
        """.trimIndent()
    }
    return """
        너는 주식 정보 서비스의 어시스턴트다. 사용자 질문에 무조건 한국어로 답하라.
        오늘 날짜는 ${LocalDate.now().format(SYSTEM_PROMPT_DATE_FORMAT)}이다. 이 날짜를 기준으로 "최신", "올해", "작년" 등을 판단하고,
        네가 학습된 시점의 지식이 이 날짜보다 오래된 정보라는 걸 감안해서 답하라.

        [엄격한 규칙] 매출, 영업이익, EPS, 주가, 시가총액 같은 구체적인 재무/시세 숫자는 아래에 제공된 뉴스에
        그 숫자가 문자 그대로 적혀 있을 때만 말하라. 뉴스에 없는 숫자는 학습된 기억에서 나온 것이라도 절대 지어내서
        말하지 마라. 그런 숫자를 모르면 "정확한 수치는 확인되지 않았습니다"라고 답변 전체에서 딱 한 번만 짧게 언급하고
        넘어가라. 존재하지도 않는 과거 분기/연도를 나열하며 같은 말을 반복하지 마라. 답변은 4~5문장 이내로 간결하게 써라.
        투자 조언이 아니라 정보 제공 목적임을 답변에 자연스럽게 반영하라.
        $newsSection
    """.trimIndent()
}

// 사용자의 평문 질문을 받아 NVIDIA NIM LLM에 질의하고 답변을 반환한다.
// 질문에 종목이 언급되면 그 종목의 최신 뉴스를 컨텍스트로 함께 넘긴다.
@Service
class StockChatService(
    private val nvidiaChatClient: NvidiaChatClient,
    private val stockRepository: StockRepository,
    private val stockNewsRepository: StockNewsRepository,
) {
    fun ask(request: StockChatRequest): StockChatResponse {
        val question = request.question
        validateQuestion(question)

        val newsContext = findNewsContext(question)
        val answer = try {
            nvidiaChatClient.chatToLLm(systemPrompt(newsContext), question)
        } catch (e: NvidiaChatException) {
            throw BusinessException(ResultCode.STOCK_CHAT_FAILED, cause = e)
        }

        return StockChatResponse(answer)
    }

    private fun findNewsContext(question: String): String? {
        val stock = stockRepository.findFirstMentionedInText(question) ?: return null
        val stockId = stock.id ?: return null
        val pageable = PageRequest.of(0, CONTEXT_NEWS_COUNT)
        val news = stockNewsRepository.findByStockIdOrderByCollectedAtDesc(stockId, pageable).content
        if (news.isEmpty()) {
            return null
        }
        val stockLabel = stockLabel(stock)
        val headlines = news.joinToString("\n") { "- ${it.title}" }
        return "[$stockLabel 관련 최신 뉴스]\n$headlines"
    }

    private fun stockLabel(stock: Stock): String =
        "${stock.ticker}(${stock.koreanName ?: stock.name})"

    private fun validateQuestion(question: String) {
        if (question.isBlank()) throw BusinessException(ResultCode.STOCK_CHAT_QUESTION_REQUIRED)
        if (question.length > QUESTION_MAX_LENGTH) throw BusinessException(ResultCode.STOCK_CHAT_QUESTION_TOO_LONG)
    }
}
