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

// 실제 NVIDIA API 대신 로컬 스텁 서버로 응답 상태코드를 통제해, 모델 단종/권한없음 시
// fallback-models로 자동 전환되는지 검증한다.
class NvidiaChatClientTest {
    private lateinit var server: HttpServer
    private val requestedModels = mutableListOf<String>()
    private val requestBodyByModel = mutableMapOf<String, String>()
    private var statusByModel = mapOf<String, Int>()
    private var nonJsonModels = setOf<String>()

    @BeforeEach
    fun startStubServer() {
        requestedModels.clear()
        requestBodyByModel.clear()
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
        requestBodyByModel[model] = body

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

    private fun client(
        model: String,
        fallbackModels: String,
        reasoningEffort: String = "",
        reasoningEffortModels: String = "",
    ) = NvidiaChatClient(
        baseUrl = "http://127.0.0.1:${server.address.port}/v1",
        apiKey = "test-key",
        model = model,
        fallbackModelsRaw = fallbackModels,
        maxTokens = 128,
        topP = 0.95,
        connectTimeoutMs = 2000,
        readTimeoutMs = 5000,
        reasoningEffort = reasoningEffort,
        reasoningEffortModelsRaw = reasoningEffortModels,
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

    // reasoning_effort는 gpt-oss 계열만 이해하는 파라미터다. 모르는 모델에 보내면 400으로 거절될 수
    // 있는데 400은 폴백 대상이 아니라 그대로 실패하므로, 지원 목록에 없는 모델에는 실리면 안 된다.
    @Test
    fun `지원 목록에 있는 모델에는 reasoning_effort를 실어 보낸다`() {
        val answer = client("gpt-oss", "", reasoningEffort = "low", reasoningEffortModels = "gpt-oss")
            .chatToLLm("system", "질문")

        assertEquals("gpt-oss 답변", answer)
        assertTrue(
            requestBodyByModel["gpt-oss"]!!.contains("\"reasoning_effort\":\"low\""),
            "지원 모델인데 reasoning_effort가 빠졌다: ${requestBodyByModel["gpt-oss"]}",
        )
    }

    @Test
    fun `지원 목록에 없는 폴백 모델로 넘어가면 reasoning_effort를 빼고 보낸다`() {
        statusByModel = mapOf("gpt-oss" to 410)

        val answer = client("gpt-oss", "other-model", reasoningEffort = "low", reasoningEffortModels = "gpt-oss")
            .chatToLLm("system", "질문")

        assertEquals("other-model 답변", answer)
        assertTrue(
            requestBodyByModel["gpt-oss"]!!.contains("reasoning_effort"),
            "기본 모델에는 실려야 한다",
        )
        assertFalse(
            requestBodyByModel["other-model"]!!.contains("reasoning_effort"),
            "미지원 폴백 모델에 실려 나갔다: ${requestBodyByModel["other-model"]}",
        )
    }

    @Test
    fun `reasoning-effort 설정이 비어 있으면 지원 모델에도 보내지 않는다`() {
        client("gpt-oss", "", reasoningEffort = "", reasoningEffortModels = "gpt-oss")
            .chatToLLm("system", "질문")

        assertFalse(
            requestBodyByModel["gpt-oss"]!!.contains("reasoning_effort"),
            "설정을 비웠는데 실려 나갔다: ${requestBodyByModel["gpt-oss"]}",
        )
    }

    @Test
    fun `reasoning-effort 값의 대소문자와 앞뒤 공백은 정규화해서 보낸다`() {
        client("gpt-oss", "", reasoningEffort = "  LOW  ", reasoningEffortModels = "gpt-oss")
            .chatToLLm("system", "질문")

        assertTrue(
            requestBodyByModel["gpt-oss"]!!.contains("\"reasoning_effort\":\"low\""),
            "정규화되지 않았다: ${requestBodyByModel["gpt-oss"]}",
        )
    }

    // 허용값(low/medium/high)이 아닌 값을 보내면 NVIDIA가 4xx로 거절하고, 4xx는 폴백 대상이 아니라
    // 챗봇이 통째로 실패한다. 설정 오타 하나로 서비스가 죽지 않도록 아예 안 보내고 넘어가야 한다.
    @Test
    fun `reasoning-effort가 허용값이 아니면 보내지 않고 답변은 정상 반환한다`() {
        val answer = client("gpt-oss", "", reasoningEffort = "veryhigh", reasoningEffortModels = "gpt-oss")
            .chatToLLm("system", "질문")

        assertEquals("gpt-oss 답변", answer)
        assertFalse(
            requestBodyByModel["gpt-oss"]!!.contains("reasoning_effort"),
            "허용값이 아닌데 실려 나갔다: ${requestBodyByModel["gpt-oss"]}",
        )
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
