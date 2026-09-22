package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.OpenAiChatCompletionRequest
import org.kjs.stocknews.model.dto.OpenAiChatCompletionResponse
import org.kjs.stocknews.model.dto.OpenAiChatMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.UnknownContentTypeException
import java.net.SocketTimeoutException

private const val ROLE_SYSTEM = "system"
private const val ROLE_USER = "user"
private const val ERROR_BODY_LOG_LIMIT = 500

// OpenAI reasoning 가이드가 나열하는 전체 값. 모델마다 지원하는 부분집합이 달라(gpt-5-mini는 minimal 포함 여부
// 미확정) 여기서는 목록 밖의 오타만 걸러내고, 모델이 거절하면 400으로 드러나게 둔다.
private val ALLOWED_REASONING_EFFORTS = setOf("none", "minimal", "low", "medium", "high", "xhigh", "max")

// 외부 응답 본문을 그대로 로그에 남기면 개행으로 가짜 로그 줄을 끼워 넣을 수 있으므로(로그 인젝션)
// 제어문자를 공백으로 바꾸고 길이를 제한한다.
private fun sanitizeForLog(body: String): String = buildString {
    for (char in body.take(ERROR_BODY_LOG_LIMIT)) {
        if (char.isISOControl()) {
            append(' ')
        } else {
            append(char)
        }
    }
}

// OpenAI Chat Completions - LlmClient의 OpenAI 구현. llm.provider=openai 일 때만 빈으로 올라간다.
//
// GPT-5 계열은 reasoning 모델이라 요청 규격이 예전 gpt-4o와 다르다(2026-09 공식 문서 기준):
//  - max_tokens는 deprecated이고 reasoning 모델과 호환되지 않는다 -> max_completion_tokens를 쓴다
//    (보이는 답변 토큰 + 추론 토큰의 합 상한)
//  - temperature/top_p는 기본값(1) 외의 값을 보내면 400 -> 아예 보내지 않는다
//  - reasoning_effort는 모델별 기본값이 문서에 명시돼 있지 않아 명시적으로 보낸다(기본 low)
// Chat Completions는 deprecated가 아니지만 신규 프로젝트에는 Responses API가 권장되므로, 스트리밍이나
// 도구 호출이 필요해지면 그쪽으로 옮기는 편이 맞다.
//
// NvidiaChatClient와 달리 모델 폴백은 없다 - 유료 API는 모델 ID가 안정적이고 장애가 429/5xx로 명확히 온다.
@Component
@ConditionalOnProperty(prefix = "llm", name = ["provider"], havingValue = "openai")
class GptClient(
    @Value("\${openai.api.base-url:https://api.openai.com/v1}") private val baseUrl: String,
    @Value("\${openai.api.key}") private val apiKey: String,
    @Value("\${openai.api.model:gpt-5-mini}") modelRaw: String,
    @Value("\${openai.api.max-completion-tokens:1024}") private val maxCompletionTokens: Int,
    @Value("\${openai.api.reasoning-effort:low}") reasoningEffortRaw: String,
    @Value("\${openai.api.connect-timeout-ms:10000}") connectTimeoutMs: Int,
    // 사용자가 화면 앞에서 기다리는 경로라 NVIDIA(120초)보다 짧게 잡는다. 유료 API는 이 안에 답하는 게 정상이다.
    @Value("\${openai.api.read-timeout-ms:60000}") private val readTimeoutMs: Int,
) : LlmClient {
    private val log = LoggerFactory.getLogger(GptClient::class.java)

    // 배포 env가 빈 문자열로 들어오면 yml 기본값이 덮이므로 여기서 한 번 더 받친다.
    private val model: String = modelRaw.ifBlank { "gpt-5-mini" }

    // 설정값이 잘못돼도 기동을 막지 않는다 - 파라미터를 보내지 않으면 모델 기본값으로 동작한다.
    private val reasoningEffort: String? = normalizeReasoningEffort(reasoningEffortRaw)

    // NvidiaChatClient와 같은 이유로 HttpURLConnection 기반 팩토리를 쓴다(JDK HttpClient의 POST 바디 전송 오류 회피).
    private val restClient = RestClient.builder()
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(connectTimeoutMs)
                setReadTimeout(readTimeoutMs)
            },
        )
        .build()

    override fun chat(systemPrompt: String, userMessage: String): String {
        val request = OpenAiChatCompletionRequest(
            model = model,
            messages = listOf(
                OpenAiChatMessage(role = ROLE_SYSTEM, content = systemPrompt),
                OpenAiChatMessage(role = ROLE_USER, content = userMessage),
            ),
            maxCompletionTokens = maxCompletionTokens,
            reasoningEffort = reasoningEffort,
        )

        val response = try {
            restClient.post()
                .uri("$baseUrl/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "application/json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OpenAiChatCompletionResponse::class.java)
        } catch (e: HttpStatusCodeException) {
            // 429는 rate limit뿐 아니라 크레딧 소진(insufficient_quota)도 같은 코드로 온다 - 본문의 error.type/code가
            // 갈라주므로 본문을 함께 남긴다. Retry-After를 따르는 재시도는 두지 않는다(사용자를 더 기다리게 할 뿐이다).
            throw LlmException(
                "openai chat completion rejected: model=$model status=${e.statusCode} " +
                    "body=${sanitizeForLog(e.responseBodyAsString)}",
                e,
            )
        } catch (e: UnknownContentTypeException) {
            throw LlmException(
                "openai chat completion returned unparseable content: model=$model contentType=${e.contentType}",
                e,
            )
        } catch (e: ResourceAccessException) {
            val isTimeout = generateSequence(e as Throwable) { it.cause }.any { it is SocketTimeoutException }
            val reason = if (isTimeout) {
                "timed out"
            } else {
                "unreachable"
            }
            throw LlmException("openai chat completion $reason: model=$model readTimeoutMs=$readTimeoutMs", e)
        } catch (e: RestClientException) {
            throw LlmException("openai chat completion failed: model=$model", e)
        }

        val choice = response?.choices?.firstOrNull()
        val content = choice?.message?.content
        if (content.isNullOrBlank()) {
            // finish_reason=length면 max_completion_tokens를 추론에 다 쓴 것, content_filter면 필터에 걸린 것.
            val finishReason = sanitizeForLog(choice?.finishReason ?: "null")
            throw LlmException("openai chat completion returned empty content: model=$model finishReason=$finishReason")
        }
        return content
    }

    private fun normalizeReasoningEffort(raw: String): String? {
        val normalized = raw.trim().lowercase()
        if (normalized.isEmpty()) {
            return null
        }
        if (normalized !in ALLOWED_REASONING_EFFORTS) {
            log.warn(
                "unknown openai.api.reasoning-effort={}, sending none (allowed: {})",
                sanitizeForLog(raw),
                ALLOWED_REASONING_EFFORTS,
            )
            return null
        }
        return normalized
    }
}
