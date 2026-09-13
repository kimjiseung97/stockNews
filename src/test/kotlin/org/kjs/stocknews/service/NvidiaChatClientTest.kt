package org.kjs.stocknews.service

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

// 실제 NVIDIA API 대신 로컬 스텁 서버로 응답 상태코드를 통제해, 모델 단종/권한없음 시
// fallback-models로 자동 전환되는지 검증한다.
class NvidiaChatClientTest {
    private lateinit var server: HttpServer
    private val requestedModels = mutableListOf<String>()
    private var statusByModel = mapOf<String, Int>()
    private var nonJsonModels = setOf<String>()

    @BeforeEach
    fun startStubServer() {
        requestedModels.clear()
        statusByModel = emptyMap()
        nonJsonModels = emptySet()
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/chat/completions") { exchange -> handle(exchange) }
        server.start()
    }

    @AfterEach
    fun stopStubServer() {
        server.stop(0)
    }

    private fun handle(exchange: HttpExchange) {
        val body = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
        val model = Regex("\"model\":\"([^\"]+)\"").find(body)?.groupValues?.get(1) ?: ""
        requestedModels.add(model)

        val status = statusByModel[model] ?: 200
        val response = when {
            model in nonJsonModels -> "not json at all"
            status == 200 -> """{"choices":[{"message":{"role":"assistant","content":"$model 답변"}}]}"""
            else -> """{"status":$status,"detail":"$model unavailable"}"""
        }
        val contentType = if (model in nonJsonModels) {
            "application/octet-stream"
        } else {
            "application/json"
        }
        val bytes = response.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", contentType)
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun client(model: String, fallbackModels: String) = NvidiaChatClient(
        baseUrl = "http://127.0.0.1:${server.address.port}/v1",
        apiKey = "test-key",
        model = model,
        fallbackModelsRaw = fallbackModels,
        maxTokens = 128,
        topP = 0.95,
        connectTimeoutMs = 2000,
        readTimeoutMs = 5000,
    )

    @Test
    fun `기본 모델이 단종(410)되면 폴백 모델로 전환해 답변을 반환한다`() {
        statusByModel = mapOf("dead-model" to 410)

        val answer = client("dead-model", "live-model").chatToLLm("system", "질문")

        assertEquals("live-model 답변", answer)
        assertEquals(listOf("dead-model", "live-model"), requestedModels)
    }

    @Test
    fun `기본 모델이 계정 권한 없음(404)이면 폴백 모델로 전환한다`() {
        statusByModel = mapOf("no-access-model" to 404)

        val answer = client("no-access-model", "live-model").chatToLLm("system", "질문")

        assertEquals("live-model 답변", answer)
    }

    @Test
    fun `폴백 모델도 못 쓰면 그 다음 폴백 모델까지 순서대로 시도한다`() {
        statusByModel = mapOf("dead-1" to 410, "dead-2" to 404)

        val answer = client("dead-1", "dead-2, live-model").chatToLLm("system", "질문")

        assertEquals("live-model 답변", answer)
        assertEquals(listOf("dead-1", "dead-2", "live-model"), requestedModels)
    }

    @Test
    fun `모든 후보 모델을 못 쓰면 마지막 실패 원인을 담은 예외를 던진다`() {
        statusByModel = mapOf("dead-1" to 410, "dead-2" to 410)

        val exception = assertThrows<NvidiaChatException> {
            client("dead-1", "dead-2").chatToLLm("system", "질문")
        }

        assertTrue(exception.message!!.contains("dead-2"))
        assertEquals(listOf("dead-1", "dead-2"), requestedModels)
    }

    @Test
    fun `모델 추론 백엔드 장애(500)면 폴백 모델로 전환한다`() {
        statusByModel = mapOf("broken-model" to 500)

        val answer = client("broken-model", "live-model").chatToLLm("system", "질문")

        assertEquals("live-model 답변", answer)
        assertEquals(listOf("broken-model", "live-model"), requestedModels)
    }

    @Test
    fun `응답을 파싱할 수 없으면 폴백하지 않고 즉시 실패한다`() {
        nonJsonModels = setOf("garbage-model")

        assertThrows<NvidiaChatException> {
            client("garbage-model", "live-model").chatToLLm("system", "질문")
        }

        assertEquals(listOf("garbage-model"), requestedModels)
    }
}
