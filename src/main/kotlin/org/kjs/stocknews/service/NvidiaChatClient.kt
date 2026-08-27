package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.NvidiaChatCompletionRequest
import org.kjs.stocknews.model.dto.NvidiaChatCompletionResponse
import org.kjs.stocknews.model.dto.NvidiaChatMessage
import org.springframework.beans.factory.annotation.Value
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

// NVIDIA NIM(build.nvidia.com) OpenAI 호환 chat completions API 클라이언트.
@Component
class NvidiaChatClient(
    @Value("\${nvidia.api.base-url}") private val baseUrl: String,
    @Value("\${nvidia.api.key}") private val apiKey: String,
    @Value("\${nvidia.api.model}") private val model: String,
    @Value("\${nvidia.api.max-tokens:1024}") private val maxTokens: Int,
    @Value("\${nvidia.api.top-p:0.95}") private val topP: Double,
    // kimi-k3는 reasoning 모델이라 응답에 60초 이상 걸리는 경우가 있어(reasoning 시간이 readTimeout을
    // 넘기면 응답이 중간에 끊겨 UnknownContentTypeException으로 위장되어 나타남) 여유를 두고 기본 120초.
    @Value("\${nvidia.api.connect-timeout-ms:10000}") private val connectTimeoutMs: Int,
    @Value("\${nvidia.api.read-timeout-ms:120000}") private val readTimeoutMs: Int,
) {
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
    // 담아 상위 계층에서 한 번만 로깅하도록 한다).
    fun chatToLLm(systemPrompt: String, userMessage: String): String {
        val request = NvidiaChatCompletionRequest(
            model = model,
            messages = listOf(
                NvidiaChatMessage(role = ROLE_SYSTEM, content = systemPrompt),
                NvidiaChatMessage(role = ROLE_USER, content = userMessage),
            ),
            temperature = 0.5,
            topP = topP,
            maxTokens = maxTokens,
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
            throw NvidiaChatException(
                "nvidia chat completion rejected: model=$model status=${e.statusCode} " +
                    "body=${e.responseBodyAsString.take(ERROR_BODY_LOG_LIMIT)}",
                e,
            )
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
            throw NvidiaChatException(
                "nvidia chat completion ${if (isTimeout) "timed out" else "unreachable"}: " +
                    "model=$model readTimeoutMs=$readTimeoutMs",
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
