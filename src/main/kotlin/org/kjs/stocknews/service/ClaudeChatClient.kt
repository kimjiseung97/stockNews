package org.kjs.stocknews.service

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.errors.AnthropicException
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Duration

// 외부 응답값(stop_reason 등)을 로그에 실을 때 개행/제어문자를 걷어낸다(로그 인젝션 방지).
private fun sanitizeForLog(text: String): String = buildString {
    for (char in text.take(200)) {
        if (char.isISOControl()) {
            append(' ')
        } else {
            append(char)
        }
    }
}

// Anthropic Claude - LlmClient의 Anthropic 구현. 공식 Java SDK(anthropic-java)를 쓴다.
// llm.provider=anthropic 일 때만 빈으로 올라간다.
//
// NvidiaChatClient와 달리 모델 폴백이 없다. NVIDIA 무료 티어는 모델이 예고 없이 단종/거절되는 일이 잦아
// 폴백이 필요했지만, 유료 API는 모델 ID가 안정적이고 장애는 429/5xx로 명확히 오므로 여기서는 실패를
// 그대로 LlmException으로 올려 상위가 한 번에 판단하게 한다. 재시도는 SDK 기본 동작(maxRetries)에 맡긴다.
@Component
@ConditionalOnProperty(prefix = "llm", name = ["provider"], havingValue = "anthropic")
class ClaudeChatClient(
    @Value("\${anthropic.api.key}") apiKey: String,
    // 테스트에서 로컬 스텁 서버를 가리키게 하려고 뺐다. 운영은 기본값(공식 엔드포인트)이다.
    @Value("\${anthropic.api.base-url:https://api.anthropic.com}") baseUrl: String,
    @Value("\${anthropic.api.model:claude-opus-5}") modelRaw: String,
    @Value("\${anthropic.api.max-tokens:1024}") private val maxTokens: Long,
    // 추론 깊이. 뉴스 근거를 읽고 요약하는 챗봇은 low로도 충분하고 응답이 빨라 기본 low.
    // low/medium/high/xhigh/max. 잘못된 값이면 기동을 막지 않고 파라미터를 보내지 않는다(모델 기본값).
    @Value("\${anthropic.api.effort:low}") effortRaw: String,
    // SDK 기본 타임아웃은 10분이라 사용자가 화면 앞에서 기다리는 경로에는 너무 길다.
    @Value("\${anthropic.api.timeout-ms:60000}") private val timeoutMs: Long,
    // 0이 기본. SDK 재시도는 Retry-After/backoff를 따르므로 1회만 해도 총 대기가 timeout-ms를 훌쩍 넘을 수 있다 -
    // 사용자가 화면 앞에서 기다리는 동기 경로에는 맞지 않아 끄고, 실패는 바로 위로 올린다.
    @Value("\${anthropic.api.max-retries:0}") maxRetries: Int,
) : LlmClient {
    private val log = LoggerFactory.getLogger(ClaudeChatClient::class.java)

    // 배포 env가 빈 문자열로 들어오면 yml 기본값이 덮이므로 여기서 한 번 더 받친다.
    private val model: String = modelRaw.ifBlank { "claude-opus-5" }

    private val client: AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .baseUrl(baseUrl)
        .timeout(Duration.ofMillis(timeoutMs))
        .maxRetries(maxRetries)
        .build()

    private val effort: OutputConfig.Effort? = parseEffort(effortRaw)

    override fun chat(systemPrompt: String, userMessage: String): String {
        val builder = MessageCreateParams.builder()
            .model(model)
            .maxTokens(maxTokens)
            .system(systemPrompt)
            .addUserMessage(userMessage)
        if (effort != null) {
            builder.outputConfig(OutputConfig.builder().effort(effort).build())
        }
        // thinking은 명시하지 않는다 - Claude Opus 5 계열은 생략하면 adaptive로 동작하고,
        // 깊이는 위 effort로 조절한다. budget_tokens 방식은 최신 모델에서 400으로 거절된다.
        val params = builder.build()

        try {
            val response = client.messages().create(params)

            // 안전 분류기가 거절하면 HTTP 200 + stop_reason=refusal로 온다. 빈 답변을 정상 응답처럼 내리지 않도록
            // content를 읽기 전에 먼저 본다.
            val stopReason = response.stopReason()
            if (stopReason.isPresent && stopReason.get() == StopReason.REFUSAL) {
                val category = response.stopDetails().flatMap { it.category() }.map { it.toString() }.orElse("null")
                throw LlmException("anthropic message refused: model=$model category=${sanitizeForLog(category)}")
            }

            val answer = buildString {
                for (block in response.content()) {
                    block.text().ifPresent { append(it.text()) }
                }
            }
            if (answer.isBlank()) {
                val reason = stopReason.map { it.toString() }.orElse("null")
                throw LlmException("anthropic message returned empty content: model=$model stopReason=${sanitizeForLog(reason)}")
            }
            return answer
        } catch (e: AnthropicServiceException) {
            // 상태코드와 에러 타입(rate_limit_error, overloaded_error, not_found_error ...)을 메시지에 남긴다.
            val errorType = e.errorType().map { it.toString() }.orElse("unknown")
            throw LlmException(
                "anthropic message rejected: model=$model status=${e.statusCode()} type=${sanitizeForLog(errorType)}",
                e,
            )
        } catch (e: AnthropicIoException) {
            throw LlmException("anthropic message unreachable or timed out: model=$model timeoutMs=$timeoutMs", e)
        } catch (e: AnthropicException) {
            // 응답 역직렬화 실패(AnthropicInvalidDataException) 등 SDK의 나머지 예외. LlmClient 계약상
            // 여기서 나가는 실패는 전부 LlmException이어야 StockChatService가 잡을 수 있다.
            throw LlmException("anthropic message failed: model=$model", e)
        }
    }

    private fun parseEffort(raw: String): OutputConfig.Effort? {
        return when (raw.trim().lowercase()) {
            "" -> null
            "low" -> OutputConfig.Effort.LOW
            "medium" -> OutputConfig.Effort.MEDIUM
            "high" -> OutputConfig.Effort.HIGH
            "xhigh" -> OutputConfig.Effort.XHIGH
            "max" -> OutputConfig.Effort.MAX
            else -> {
                log.warn("unknown anthropic.api.effort={}, sending none", sanitizeForLog(raw))
                null
            }
        }
    }
}
