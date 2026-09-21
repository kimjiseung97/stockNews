package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.NvidiaChatCompletionRequest
import org.kjs.stocknews.model.dto.NvidiaChatCompletionResponse
import org.kjs.stocknews.model.dto.NvidiaChatMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
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

// NVIDIA가 받는 reasoning_effort 값은 이 셋뿐이다. 다른 값을 보내면 4xx로 거절당하는데,
// 4xx는 폴백 대상이 아니라(모델 문제가 아니라 요청 문제로 본다) 그대로 실패한다.
// 즉 오타 하나로 챗봇이 통째로 멈추므로 보내기 전에 걸러야 한다.
private val ALLOWED_REASONING_EFFORTS = setOf("low", "medium", "high")

// 외부 API 응답 본문을 그대로 로그에 남기면 개행 문자로 가짜 로그 줄을 끼워 넣을 수 있으므로(로그 인젝션)
// 개행/제어문자를 공백으로 바꾸고 길이를 제한한다.
private fun sanitizeForLog(body: String): String {
    val truncated = body.take(ERROR_BODY_LOG_LIMIT)
    return buildString {
        for (char in truncated) {
            if (char.isISOControl()) {
                append(' ')
            } else {
                append(char)
            }
        }
    }
}

// NVIDIA NIM(build.nvidia.com) OpenAI 호환 chat completions API 클라이언트.
@Component
class NvidiaChatClient(
    @Value("\${nvidia.api.base-url}") private val baseUrl: String,
    @Value("\${nvidia.api.key}") private val apiKey: String,
    @Value("\${nvidia.api.model}") private val model: String,
    // NVIDIA NIM은 모델을 예고 없이 단종(410)시키고, 계정에 호출 권한이 없는 모델은 404, 추론 백엔드가
    // 죽은 모델은 5xx를 준다(meta/llama-3.1-8b-instruct 단종으로 챗봇이 완전히 멈춘 적 있음).
    // 기본 모델이 이렇게 실패하면 이 목록의 모델로 순서대로 자동 전환해 단일 모델 문제가 서비스 전체
    // 장애로 이어지지 않게 한다. 응답이 readTimeout을 넘길 만큼 느린 모델은 폴백해도 대기만 길어지므로
    // 목록에 넣지 말 것(그래서 moonshotai/kimi-k3는 제외됨).
    @Value("\${nvidia.api.fallback-models:}") private val fallbackModelsRaw: String,
    @Value("\${nvidia.api.max-tokens:1024}") private val maxTokens: Int,
    @Value("\${nvidia.api.top-p:0.95}") private val topP: Double,
    // reasoning 계열 모델은 응답에 60초 이상 걸리는 경우가 있어(reasoning 시간이 readTimeout을 넘기면
    // 응답이 중간에 끊겨 UnknownContentTypeException으로 위장되어 나타남) 여유를 두고 기본 120초.
    @Value("\${nvidia.api.connect-timeout-ms:10000}") private val connectTimeoutMs: Int,
    @Value("\${nvidia.api.read-timeout-ms:120000}") private val readTimeoutMs: Int,
    // 답을 내기 전에 태우는 추론 토큰 분량(low/medium/high). gpt-oss 계열은 이 값을 안 보내면
    // 기본값으로 길게 추론한다 - 실측에서 답변 하나에 41초가 걸렸고 그 대부분이 추론이었다.
    // 빈 값이면 파라미터를 아예 보내지 않아 모델 기본 동작으로 돌아간다.
    @Value("\${nvidia.api.reasoning-effort:}") private val reasoningEffort: String,
    // reasoning_effort를 실어 보낼 모델 목록. 모르는 파라미터를 받으면 400으로 거절하는 모델이
    // 있는데, 400은 폴백 대상이 아니라(모델 문제가 아니라 요청 문제로 본다) 그대로 실패한다.
    // 그래서 기본 모델이 죽어 폴백으로 넘어갔을 때 이 파라미터가 따라가 챗봇을 통째로 멈추지
    // 않도록, 지원이 확인된 모델에만 싣는다.
    @Value("\${nvidia.api.reasoning-effort-models:}") private val reasoningEffortModelsRaw: String,
) {
    private val log = LoggerFactory.getLogger(NvidiaChatClient::class.java)

    private val candidateModels: List<String> = (listOf(model) + fallbackModelsRaw.split(","))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

    // 설정값을 정규화하고 검증해 둔다. 허용값이 아니면 파라미터를 보내지 않는다 - 여기서 예외를
    // 던져 기동을 막으면 챗봇 튜닝용 설정 하나 때문에 검색/메일/배치까지 전부 내려간다.
    // 값이 잘못돼도 서비스는 살고 응답만 예전 속도로 돌아가는 쪽이 낫다.
    private val normalizedReasoningEffort: String? = normalizeReasoningEffort(reasoningEffort)

    private val reasoningEffortModels: Set<String> = reasoningEffortModelsRaw.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    // JdkClientHttpRequestFactory(java.net.http.HttpClient)가 POST 요청 바디 전송 시 "Request cancelled" I/O
    // 에러를 일으켜, HttpURLConnection 기반의 SimpleClientHttpRequestFactory로 대체.
    private val restClient = RestClient.builder()
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(connectTimeoutMs)
                setReadTimeout(readTimeoutMs)
            },
        )
        .build()

    // 실패 시 NvidiaChatException을 던진다(로깅은 여기서 하지 않고, 진단에 필요한 정보를 예외 메시지에
    // 담아 상위 계층에서 한 번만 로깅하도록 한다). 단, 모델을 못 쓰는 경우(410/404)는 여기서 WARN으로
    // 남기고 candidateModels의 다음 모델로 자동 재시도한다.
    fun chatToLLm(systemPrompt: String, userMessage: String): String {
        var lastError: NvidiaChatException? = null

        for ((index, candidateModel) in candidateModels.withIndex()) {
            try {
                return callModel(candidateModel, systemPrompt, userMessage)
            } catch (e: NvidiaModelUnavailableException) {
                lastError = e
                if (index < candidateModels.lastIndex) {
                    log.warn(
                        "nvidia model unavailable, falling back: model={} next={}",
                        candidateModel,
                        candidateModels[index + 1],
                    )
                }
            }
        }

        if (lastError != null) {
            throw lastError
        }
        throw NvidiaChatException("no nvidia model configured")
    }

    private fun normalizeReasoningEffort(raw: String): String? {
        val normalized = raw.trim().lowercase()
        if (normalized.isEmpty()) {
            return null
        }
        if (normalized !in ALLOWED_REASONING_EFFORTS) {
            log.warn(
                "unknown nvidia.api.reasoning-effort={}, sending none (allowed: {})",
                sanitizeForLog(raw),
                ALLOWED_REASONING_EFFORTS,
            )
            return null
        }
        return normalized
    }

    // 이 모델에 실어 보낼 reasoning_effort. 설정이 비었거나 허용값이 아니거나 지원 목록에 없는
    // 모델이면 null이고, null이면 DTO 직렬화에서 키가 통째로 빠진다.
    private fun reasoningEffortFor(model: String): String? {
        if (normalizedReasoningEffort == null) {
            return null
        }
        if (model !in reasoningEffortModels) {
            return null
        }
        return normalizedReasoningEffort
    }

    private fun callModel(model: String, systemPrompt: String, userMessage: String): String {
        val request = NvidiaChatCompletionRequest(
            model = model,
            messages = listOf(
                NvidiaChatMessage(role = ROLE_SYSTEM, content = systemPrompt),
                NvidiaChatMessage(role = ROLE_USER, content = userMessage),
            ),
            temperature = 0.5,
            topP = topP,
            maxTokens = maxTokens,
            reasoningEffort = reasoningEffortFor(model),
        )

        val response = try {
            restClient.post()
                .uri("$baseUrl/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "application/json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(NvidiaChatCompletionResponse::class.java)
        } catch (e: HttpStatusCodeException) {
            val message = "nvidia chat completion rejected: model=$model status=${e.statusCode} " +
                "body=${sanitizeForLog(e.responseBodyAsString)}"
            // 410(단종), 404("Function ... Not found for account" - 계정에 호출 권한 없음),
            // 5xx("Inference connection error" - 그 모델 추론 백엔드 장애)는 모두 그 모델만의 문제라
            // 다른 모델로 넘기면 살아난다(응답도 빨리 와서 폴백 비용이 작음).
            // 반면 타임아웃/응답 파싱 실패는 폴백해도 대기시간만 배로 늘어나므로 즉시 실패시킨다.
            val status = e.statusCode
            val modelUnusable = status == HttpStatus.GONE ||
                status == HttpStatus.NOT_FOUND ||
                status.is5xxServerError
            if (modelUnusable) {
                throw NvidiaModelUnavailableException(message, e)
            }
            throw NvidiaChatException(message, e)
        } catch (e: UnknownContentTypeException) {
            // 응답은 왔지만 Content-Type이 JSON이 아니거나 누락된 경우(주로 readTimeout 근접/네트워크
            // 중단 시 응답이 중간에 끊기면서 발생). 실제 원인은 대부분 타임아웃이므로 readTimeoutMs를 같이 남긴다.
            throw NvidiaChatException(
                "nvidia chat completion returned unparseable content: model=$model " +
                    "contentType=${e.contentType} readTimeoutMs=$readTimeoutMs",
                e,
            )
        } catch (e: ResourceAccessException) {
            val isTimeout = generateSequence(e as Throwable) { it.cause }.any { it is SocketTimeoutException }
            val reason = if (isTimeout) {
                "timed out"
            } else {
                "unreachable"
            }
            throw NvidiaChatException(
                "nvidia chat completion $reason: model=$model readTimeoutMs=$readTimeoutMs",
                e,
            )
        } catch (e: RestClientException) {
            throw NvidiaChatException("nvidia chat completion failed: model=$model", e)
        }

        val content = response?.choices?.firstOrNull()?.message?.content
        if (content.isNullOrBlank()) {
            throw NvidiaChatException("nvidia chat completion returned empty content: model=$model")
        }
        return content
    }
}
