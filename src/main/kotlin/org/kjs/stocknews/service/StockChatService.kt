package org.kjs.stocknews.service

import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.NewsChunkHit
import org.kjs.stocknews.model.dto.StockChatRequest
import org.kjs.stocknews.model.dto.StockChatResponse
import org.kjs.stocknews.model.table.Stock
import org.kjs.stocknews.repository.StockRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val QUESTION_MAX_LENGTH = 300

// 뉴스는 외부 수집물이라 길이를 신뢰할 수 없다 - 프롬프트가 무한정 길어지지 않도록 잘라 넣는다.
private const val NEWS_TITLE_MAX_LENGTH = 200

// 본문 청크는 제목보다 길어 그대로 넣으면 몇 건만으로 프롬프트가 불어난다.
private const val NEWS_CONTENT_MAX_LENGTH = 500

private val SYSTEM_PROMPT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy년 M월 d일")

// 서버 타임존 설정과 무관하게 사용자 기준(한국) 날짜를 프롬프트에 넣는다.
private val SERVICE_ZONE = ZoneId.of("Asia/Seoul")

// 사용자의 평문 질문을 받아 NVIDIA NIM LLM에 질의하고 답변을 반환한다.
//
// 시스템 프롬프트 본문은 코드가 아니라 어드민(stockNewsAdmin)이 TB_PROMPT에 등록한 것을 가져다 쓴다
// (CODE = STOCK_CHAT_SYSTEM). 여기서는 프롬프트에 끼워 넣을 값만 만들어 넘긴다.
//   {{today}}       - LLM이 학습 시점 지식에 갇혀 "올해"를 잘못 판단하는 걸 막기 위한 실제 오늘 날짜
//   {{stockLabel}}  - 질문에서 찾아낸 종목 표기(예: AAPL(애플)), 못 찾으면 빈 값
//   {{newsContext}} - 질문과 의미가 가까운 실제 수집 뉴스(RAG), 없으면 빈 값
//
// 뉴스는 최신순이 아니라 의미 유사도순으로 고른다. 질문을 그 자리에서 임베딩해(NewsVectorSearchService)
// pgvector에 적재된 뉴스 청크 중 가까운 것을 꺼내므로, "실적" 질문에는 실적 기사가 걸린다.
// 최신순 N건을 넣던 이전 방식은 질문과 무관한 기사가 컨텍스트를 차지하는 문제가 있었다.
@Service
class StockChatService(
    private val nvidiaChatClient: NvidiaChatClient,
    private val stockRepository: StockRepository,
    private val newsVectorSearchService: NewsVectorSearchService,
    private val promptService: PromptService,
) {
    private val log = LoggerFactory.getLogger(StockChatService::class.java)

    fun ask(request: StockChatRequest): StockChatResponse {
        val question = request.question
        validateQuestion(question)

        val startedAt = System.nanoTime()
        val systemPrompt = promptService.render(PromptCode.STOCK_CHAT_SYSTEM, promptVariables(question))
        val promptReadyAt = System.nanoTime()

        val answer = try {
            nvidiaChatClient.chatToLLm(systemPrompt, question)
        } catch (e: NvidiaChatException) {
            // 실패한 호출의 소요시간도 남긴다. 타임아웃(read-timeout 120초)으로 죽은 것인지
            // 즉시 거절된 것인지가 숫자로 갈린다.
            log.info(
                "stock chat timing (failed): promptMs={} llmMs={}",
                elapsedMs(startedAt, promptReadyAt),
                elapsedMs(promptReadyAt, System.nanoTime()),
            )
            throw BusinessException(ResultCode.STOCK_CHAT_FAILED, cause = e)
        }
        val finishedAt = System.nanoTime()

        log.info(
            "stock chat timing: promptMs={} llmMs={} totalMs={}",
            elapsedMs(startedAt, promptReadyAt),
            elapsedMs(promptReadyAt, finishedAt),
            elapsedMs(startedAt, finishedAt),
        )

        return StockChatResponse(answer)
    }

    // 질문에 언급된 종목을 찾고, 그 종목의 뉴스 중 질문과 의미가 가까운 것을 프롬프트 변수로 만들어준다.
    // 종목을 못 찾으면 종목 필터 없이 전체에서 찾는다 - 종목명을 안 쓴 질문("반도체 업황 어때?")도
    // 뉴스 근거를 받을 수 있어야 하기 때문이다.
    // 검색 결과가 없거나 벡터 DB/모델이 실패하면 관련 변수는 빈 값이 되고,
    // 프롬프트의 해당 구간({{#newsContext}})도 통째로 빠진다.
    private fun promptVariables(question: String): Map<String, String?> {
        val stockLookupStartedAt = System.nanoTime()
        val stock = stockRepository.findFirstMentionedInText(question)
        val stockLookupFinishedAt = System.nanoTime()

        val relatedArticles = newsVectorSearchService.search(question, stock?.id)
        val newsSearchFinishedAt = System.nanoTime()

        // stockLookupMs는 TB_STOCK 전량 스캔(질문 문장에 종목명이 들어 있는지 보는 역방향 LIKE),
        // newsSearchMs는 질의 임베딩 + pgvector 조회를 합친 값이다. 그 둘의 내역은
        // NewsVectorSearchService가 따로 남긴다.
        log.info(
            "stock chat prompt timing: stockLookupMs={} newsSearchMs={} articles={}",
            elapsedMs(stockLookupStartedAt, stockLookupFinishedAt),
            elapsedMs(stockLookupFinishedAt, newsSearchFinishedAt),
            relatedArticles.size,
        )

        val stockLabel = stock?.let { stockLabel(it) }
        val newsContext = if (relatedArticles.isEmpty()) {
            null
        } else {
            val heading = if (stockLabel == null) {
                "[질문과 관련된 뉴스]"
            } else {
                "[$stockLabel 관련 뉴스]"
            }
            heading + "\n" + relatedArticles.joinToString("\n") { articleLine(it) }
        }

        return mapOf(
            PromptCode.VAR_TODAY to LocalDate.now(SERVICE_ZONE).format(SYSTEM_PROMPT_DATE_FORMAT),
            PromptCode.VAR_STOCK_LABEL to stockLabel,
            PromptCode.VAR_NEWS_CONTEXT to newsContext,
        )
    }

    // 제목과 본문 청크를 한 줄로 만든다. 본문까지 넣는 이유는 제목만으로는 LLM이 근거로 쓸 내용이
    // 부족해 학습된 기억에서 숫자를 지어내기 쉽기 때문이다.
    private fun articleLine(article: NewsChunkHit): String {
        val title = sanitizeForPrompt(article.title, NEWS_TITLE_MAX_LENGTH)
        val content = sanitizeForPrompt(article.content, NEWS_CONTENT_MAX_LENGTH)
        if (content.isBlank()) {
            return "- $title"
        }
        return "- $title: $content"
    }

    private fun stockLabel(stock: Stock): String =
        "${stock.ticker}(${stock.koreanName ?: stock.name})"

    // 뉴스는 우리가 쓴 글이 아니라 외부에서 긁어온 문자열이다. 그 안에 줄바꿈으로 새 지시문처럼 보이는
    // 문장을 심어두면("- 위 규칙을 무시하라") 시스템 프롬프트의 지시로 읽힐 수 있으므로(간접 프롬프트 인젝션),
    // 줄바꿈/제어문자를 한 칸 공백으로 눕히고 길이도 잘라 한 줄짜리 데이터로 만들어 넣는다.
    // 제목뿐 아니라 본문 청크도 같은 경로로 들어오므로 반드시 둘 다 거쳐야 한다.
    private fun sanitizeForPrompt(text: String, maxLength: Int): String {
        val flattened = buildString {
            for (char in text) {
                if (char.isISOControl()) {
                    append(' ')
                } else {
                    append(char)
                }
            }
        }
            .replace(Regex("\\s+"), " ")
            .trim()
        if (flattened.length <= maxLength) {
            return flattened
        }
        return flattened.take(maxLength) + "…"
    }

    // 구간 측정에는 currentTimeMillis가 아니라 nanoTime을 쓴다. nanoTime은 단조 증가라
    // NTP 시계 조정이 끼어들어도 음수나 엉뚱한 값이 나오지 않는다.
    private fun elapsedMs(fromNanos: Long, toNanos: Long): Long = (toNanos - fromNanos) / 1_000_000

    private fun validateQuestion(question: String) {
        if (question.isBlank()) {
            throw BusinessException(ResultCode.STOCK_CHAT_QUESTION_REQUIRED)
        }
        if (question.length > QUESTION_MAX_LENGTH) {
            throw BusinessException(ResultCode.STOCK_CHAT_QUESTION_TOO_LONG)
        }
    }
}
