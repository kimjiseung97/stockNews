package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.NvidiaChatCompletionRequest
import org.kjs.stocknews.model.dto.NvidiaChatCompletionResponse
import org.kjs.stocknews.model.dto.NvidiaChatMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

// NVIDIA NIM(build.nvidia.com) OpenAI 호환 chat completions API 클라이언트.
@Component
class NvidiaChatClient(
    @Value("\${nvidia.api.base-url}") private val baseUrl: String,
    @Value("\${nvidia.api.key}") private val apiKey: String,
    @Value("\${nvidia.api.model}") private val model: String,
    @Value("\${nvidia.api.max-tokens:1024}") private val maxTokens: Int,
    @Value("\${nvidia.api.top-p:0.95}") private val topP: Double,
) {
    private val log = LoggerFactory.getLogger(NvidiaChatClient::class.java)

    // JdkClientHttpRequestFactory(java.net.http.HttpClient)가 POST 요청 바디 전송 시 "Request cancelled" I/O
    // 에러를 일으켜, HttpURLConnection 기반의 SimpleClientHttpRequestFactory로 대체.
    // kimi-k3는 reasoning 모델이라 응답에 60초 이상 걸리는 경우가 있어(기존 60초 readTimeout에 걸리면
    // 응답이 중간에 끊겨 Content-Type이 유실되고 "application/octet-stream"으로 오인되는 증상으로 나타남),
    // 여유를 두고 120초로 설정.
    private val restClient = RestClient.builder()
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(10_000)
                setReadTimeout(120_000)
            },
        )
        .build()

    fun chatToLLm(systemPrompt: String, userMessage: String): String? {
        val request = NvidiaChatCompletionRequest(
            model = model,
            messages = listOf(
                NvidiaChatMessage(role = "system", content = systemPrompt),
                NvidiaChatMessage(role = "user", content = userMessage),
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
        } catch (e: Exception) {
            log.warn("nvidia chat completion failed", e)
            null
        }

        return response?.choices?.firstOrNull()?.message?.content
    }
}
