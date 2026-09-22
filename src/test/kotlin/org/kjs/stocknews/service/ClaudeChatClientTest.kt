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

// Anthropic SDK의 baseUrl을 로컬 스텁으로 돌려 요청 형식과 거절/오류 처리를 검증한다.
class ClaudeChatClientTest {
    private lateinit var server: HttpServer
    private var lastRequestBody = ""
    private var requestCount = 0
    private var responseStatus = 200
    private var responseBody = successBody("답변")

    private fun successBody(text: String) =
        """{"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5","content":[{"type":"text","text":"$text"}],"stop_reason":"end_turn","stop_sequence":null,"usage":{"input_tokens":10,"output_tokens":5}}"""

    @BeforeEach
    fun startStubServer() {
        lastRequestBody = ""
        requestCount = 0
        responseStatus = 200
        responseBody = successBody("답변")
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/messages") { exchange -> handle(exchange) }
        server.start()
    }

    @AfterEach
    fun stopStubServer() {
        server.stop(0)
    }

    private fun handle(exchange: HttpExchange) {
        requestCount += 1
        lastRequestBody = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
        val bytes = responseBody.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(responseStatus, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun client(effort: String = "low") = ClaudeChatClient(
        apiKey = "test-key",
        baseUrl = "http://127.0.0.1:${server.address.port}",
        modelRaw = "claude-opus-5",
        maxTokens = 256,
        effortRaw = effort,
        timeoutMs = 5000,
        maxRetries = 0,
    )

    @Test
    fun `system과 user 메시지, effort를 담아 요청하고 텍스트 블록을 답변으로 돌려준다`() {
        val answer = client().chat("시스템 프롬프트", "질문")

        assertEquals("답변", answer)
        assertTrue(lastRequestBody.contains("\"model\":\"claude-opus-5\""), lastRequestBody)
        assertTrue(lastRequestBody.contains("\"system\":\"시스템 프롬프트\""), lastRequestBody)
        assertTrue(lastRequestBody.contains("\"max_tokens\":256"), lastRequestBody)
        assertTrue(lastRequestBody.contains("\"effort\":\"low\""), lastRequestBody)
        assertTrue(lastRequestBody.contains("질문"), lastRequestBody)
    }

    @Test
    fun `안전 분류기 거절(stop_reason=refusal)은 빈 답변이 아니라 실패로 끝난다`() {
        responseBody = """{"id":"msg_2","type":"message","role":"assistant","model":"claude-opus-5","content":[],"stop_reason":"refusal","stop_sequence":null,"stop_details":{"type":"refusal","category":"cyber","explanation":"declined"},"usage":{"input_tokens":10,"output_tokens":0}}"""

        val error = assertThrows<LlmException> { client().chat("시스템", "질문") }

        assertTrue(error.message!!.contains("refused"), error.message)
    }

    @Test
    fun `429면 상태코드와 에러 타입을 담은 LlmException으로 끝난다`() {
        responseStatus = 429
        responseBody = """{"type":"error","error":{"type":"rate_limit_error","message":"slow down"}}"""

        val error = assertThrows<LlmException> { client().chat("시스템", "질문") }

        assertTrue(error.message!!.contains("status=429"), error.message)
        assertEquals(1, requestCount, "maxRetries=0이면 재시도 없이 한 번만 요청해야 한다")
    }
}
