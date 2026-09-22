package org.kjs.stocknews.service

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

// 실제 OpenAI 대신 로컬 스텁 서버로 요청 규격(GPT-5 계열 제약)과 실패 처리를 검증한다.
class GptClientTest {
    private lateinit var server: HttpServer
    private var lastRequestBody = ""
    private var responseStatus = 200
    private var responseBody = """{"choices":[{"message":{"role":"assistant","content":"답변"},"finish_reason":"stop"}]}"""

    @BeforeEach
    fun startStubServer() {
        lastRequestBody = ""
        responseStatus = 200
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/chat/completions") { exchange -> handle(exchange) }
        server.start()
    }

    @AfterEach
    fun stopStubServer() {
        server.stop(0)
    }

    private fun handle(exchange: HttpExchange) {
        lastRequestBody = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
        val bytes = responseBody.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(responseStatus, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun client(reasoningEffort: String = "low") = GptClient(
        baseUrl = "http://127.0.0.1:${server.address.port}/v1",
        apiKey = "test-key",
        modelRaw = "gpt-5-mini",
        maxCompletionTokens = 256,
        reasoningEffortRaw = reasoningEffort,
        connectTimeoutMs = 2000,
        readTimeoutMs = 5000,
    )

    @Test
    fun `GPT-5 규격으로 요청한다 - max_completion_tokens와 reasoning_effort는 있고 max_tokens와 temperature는 없다`() {
        val answer = client().chat("시스템", "질문")

        assertEquals("답변", answer)
        assertTrue(lastRequestBody.contains("\"max_completion_tokens\":256"), lastRequestBody)
        assertTrue(lastRequestBody.contains("\"reasoning_effort\":\"low\""), lastRequestBody)
        assertTrue(lastRequestBody.contains("\"content\":\"시스템\""), lastRequestBody)
        assertFalse(lastRequestBody.contains("\"max_tokens\""), lastRequestBody)
        assertFalse(lastRequestBody.contains("\"temperature\""), lastRequestBody)
        assertFalse(lastRequestBody.contains("\"top_p\""), lastRequestBody)
    }

    @Test
    fun `허용되지 않은 reasoning_effort 값이면 파라미터를 보내지 않는다`() {
        client(reasoningEffort = "turbo").chat("시스템", "질문")

        assertFalse(lastRequestBody.contains("reasoning_effort"), lastRequestBody)
    }

    @Test
    fun `429(rate limit 또는 크레딧 소진)면 상태코드와 본문을 담은 LlmException으로 끝난다`() {
        responseStatus = 429
        responseBody = """{"error":{"message":"You exceeded your current quota","type":"insufficient_quota","code":"insufficient_quota"}}"""

        val error = assertThrows<LlmException> { client().chat("시스템", "질문") }

        assertTrue(error.message!!.contains("status=429"), error.message)
        assertTrue(error.message!!.contains("insufficient_quota"), error.message)
    }

    @Test
    fun `content가 null이면 finish_reason을 담은 실패로 끝난다`() {
        responseBody = """{"choices":[{"message":{"role":"assistant","content":null},"finish_reason":"length"}]}"""

        val error = assertThrows<LlmException> { client().chat("시스템", "질문") }

        assertTrue(error.message!!.contains("finishReason=length"), error.message)
    }
}
