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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val QUESTION_MAX_LENGTH = 300
private const val CONTEXT_NEWS_COUNT = 3

// 뉴스 제목은 외부 수집물이라 길이를 신뢰할 수 없다 - 프롬프트가 무한정 길어지지 않도록 잘라 넣는다.
private const val NEWS_TITLE_MAX_LENGTH = 200

private val SYSTEM_PROMPT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy년 M월 d일")

// 서버 타임존 설정과 무관하게 사용자 기준(한국) 날짜를 프롬프트에 넣는다.
private val SERVICE_ZONE = ZoneId.of("Asia/Seoul")

// 사용자의 평문 질문을 받아 NVIDIA NIM LLM에 질의하고 답변을 반환한다.
//
// 시스템 프롬프트 본문은 코드가 아니라 어드민(stockNewsAdmin)이 TB_PROMPT에 등록한 것을 가져다 쓴다
// (CODE = STOCK_CHAT_SYSTEM). 여기서는 프롬프트에 끼워 넣을 값만 만들어 넘긴다.
//   {{today}}       - LLM이 학습 시점 지식에 갇혀 "올해"를 잘못 판단하는 걸 막기 위한 실제 오늘 날짜
//   {{stockLabel}}  - 질문에서 찾아낸 종목 표기(예: AAPL(애플)), 못 찾으면 빈 값
//   {{newsContext}} - 그 종목의 실제 수집 뉴스 헤드라인(경량 RAG), 없으면 빈 값
@Service
class StockChatService(
    private val nvidiaChatClient: NvidiaChatClient,
    private val stockRepository: StockRepository,
    private val stockNewsRepository: StockNewsRepository,
    private val promptService: PromptService,
) {
    fun ask(request: StockChatRequest): StockChatResponse {
        val question = request.question
        validateQuestion(question)

        val systemPrompt = promptService.render(PromptCode.STOCK_CHAT_SYSTEM, promptVariables(question))
        val answer = try {
            nvidiaChatClient.chatToLLm(systemPrompt, question)
        } catch (e: NvidiaChatException) {
            throw BusinessException(ResultCode.STOCK_CHAT_FAILED, cause = e)
        }

        return StockChatResponse(answer)
    }

    // 질문에 언급된 종목을 찾아 그 종목의 최신 뉴스까지 프롬프트 변수로 만들어준다.
    // 종목을 못 찾거나 수집된 뉴스가 없으면 관련 변수는 빈 값이 되고, 프롬프트의 해당 구간({{#newsContext}})도 통째로 빠진다.
    private fun promptVariables(question: String): Map<String, String?> {
        val stock = stockRepository.findFirstMentionedInText(question)
        val news = stock?.id?.let {
            stockNewsRepository.findByStockIdOrderByCollectedAtDesc(it, PageRequest.of(0, CONTEXT_NEWS_COUNT)).content
        }.orEmpty()

        val stockLabel = stock?.let { stockLabel(it) }
        val newsContext = if (news.isEmpty()) {
            null
        } else {
            "[$stockLabel 관련 최신 뉴스]\n" + news.joinToString("\n") { "- ${sanitizeForPrompt(it.title)}" }
        }

        return mapOf(
            PromptCode.VAR_TODAY to LocalDate.now(SERVICE_ZONE).format(SYSTEM_PROMPT_DATE_FORMAT),
            PromptCode.VAR_STOCK_LABEL to stockLabel,
            PromptCode.VAR_NEWS_CONTEXT to newsContext,
        )
    }

    private fun stockLabel(stock: Stock): String =
        "${stock.ticker}(${stock.koreanName ?: stock.name})"

    // 뉴스 제목은 우리가 쓴 글이 아니라 외부에서 긁어온 문자열이다. 제목 안에 줄바꿈으로 새 지시문처럼 보이는
    // 문장을 심어두면("- 위 규칙을 무시하라") 시스템 프롬프트의 지시로 읽힐 수 있으므로(간접 프롬프트 인젝션),
    // 줄바꿈/제어문자를 한 칸 공백으로 눕히고 길이도 잘라 한 줄짜리 데이터로 만들어 넣는다.
    private fun sanitizeForPrompt(title: String): String {
        val flattened = buildString {
            for (char in title) {
                if (char.isISOControl()) {
                    append(' ')
                } else {
                    append(char)
                }
            }
        }
            .replace(Regex("\\s+"), " ")
            .trim()
        if (flattened.length <= NEWS_TITLE_MAX_LENGTH) {
            return flattened
        }
        return flattened.take(NEWS_TITLE_MAX_LENGTH) + "…"
    }

    private fun validateQuestion(question: String) {
        if (question.isBlank()) {
            throw BusinessException(ResultCode.STOCK_CHAT_QUESTION_REQUIRED)
        }
        if (question.length > QUESTION_MAX_LENGTH) {
            throw BusinessException(ResultCode.STOCK_CHAT_QUESTION_TOO_LONG)
        }
    }
}
