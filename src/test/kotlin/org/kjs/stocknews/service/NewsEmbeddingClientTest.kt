package org.kjs.stocknews.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kjs.stocknews.model.dto.NewsEmbeddingItem
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private const val CONNECT_TIMEOUT_MS = 2000L
private const val READ_TIMEOUT_MS = 5000L
private const val CRLF = "\r\n"
private const val HEADER_END = "\r\n\r\n"

// 임베딩 서비스 대신 원시 소켓을 열어, 클라이언트가 실제로 어떤 바이트를 보내는지 검사한다.
// 헤더 단위로 확인해야 해서 MockRestServiceServer로는 부족하다 - 그쪽은 요청을 객체로만 보여준다.
class NewsEmbeddingClientTest {
    private lateinit var serverSocket: ServerSocket

    @Volatile
    private var receivedRequest: String = ""
    private val requestReceived = CountDownLatch(1)

    @BeforeEach
    fun startServer() {
        // 포트 0은 OS가 빈 포트를 골라준다. 고정 포트를 쓰면 CI에서 충돌한다.
        serverSocket = ServerSocket(0)
        thread(isDaemon = true) {
            try {
                serverSocket.accept().use { socket ->
                    receivedRequest = readFullRequest(socket.getInputStream())
                    requestReceived.countDown()

                    val body = """{"news":1,"embedded":1,"skipped":0,"chunks":2}"""
                    val response = "HTTP/1.1 200 OK" + CRLF +
                        "Content-Type: application/json" + CRLF +
                        "Content-Length: " + body.toByteArray().size + CRLF +
                        CRLF +
                        body
                    socket.getOutputStream().write(response.toByteArray())
                    socket.getOutputStream().flush()
                }
            } catch (e: Exception) {
                requestReceived.countDown()
            }
        }
    }

    @AfterEach
    fun stopServer() {
        serverSocket.close()
    }

    // 헤더 끝까지 한 바이트씩 읽고 바디를 마저 읽는다. 헤더와 바디가 서로 다른 TCP 세그먼트로 오는
    // 경우가 있어, 한 번의 read()로 끝내면 테스트가 간헐적으로 깨진다.
    private fun readFullRequest(input: InputStream): String {
        val header = StringBuilder()
        while (!header.endsWith(HEADER_END)) {
            val byte = input.read()
            if (byte == -1) {
                return header.toString()
            }
            header.append(byte.toChar())
        }

        // Spring의 JdkClientHttpRequestFactory는 바디를 스트리밍으로 넘겨 Transfer-Encoding: chunked로 보낸다.
        // Content-Length가 붙는 경우도 있어 양쪽 다 처리한다.
        val bodyBytes = if (hasHeaderValue(header.toString(), "Transfer-Encoding", "chunked")) {
            readChunkedBody(input)
        }
        else {
            readFixedLengthBody(input, contentLengthOf(header.toString()))
        }

        // 헤더는 ASCII라 한 바이트씩 읽어도 안전하지만, 바디는 한글이 섞이므로 UTF-8로 한 번에 디코딩한다.
        return header.toString() + String(bodyBytes, Charsets.UTF_8)
    }

    private fun readFixedLengthBody(input: InputStream, contentLength: Int): ByteArray {
        if (contentLength <= 0) {
            return ByteArray(0)
        }

        val body = ByteArray(contentLength)
        var readTotal = 0
        while (readTotal < contentLength) {
            val read = input.read(body, readTotal, contentLength - readTotal)
            if (read == -1) {
                break
            }
            readTotal += read
        }
        return body.copyOf(readTotal)
    }

    // chunked는 "크기(16진수) CRLF 데이터 CRLF"의 반복이고, 크기 0인 청크가 끝을 알린다.
    private fun readChunkedBody(input: InputStream): ByteArray {
        val body = ByteArrayOutputStream()
        while (true) {
            val sizeLine = readLine(input)
            if (sizeLine == null) {
                break
            }

            // 청크 확장(";" 뒤)은 쓰지 않으므로 잘라내고 16진수만 읽는다.
            val size = sizeLine.trim().substringBefore(";").toIntOrNull(16) ?: 0
            if (size == 0) {
                break
            }

            body.write(readFixedLengthBody(input, size))
            // 청크 데이터 뒤에 붙는 CRLF를 버린다.
            readLine(input)
        }
        return body.toByteArray()
    }

    private fun readLine(input: InputStream): String? {
        val line = StringBuilder()
        while (!line.endsWith(CRLF)) {
            val byte = input.read()
            if (byte == -1) {
                if (line.isEmpty()) {
                    return null
                }
                return line.toString()
            }
            line.append(byte.toChar())
        }
        return line.toString()
    }

    private fun hasHeaderValue(header: String, name: String, value: String): Boolean {
        for (line in header.split(CRLF)) {
            if (line.startsWith("$name:", ignoreCase = true) && line.contains(value, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun contentLengthOf(header: String): Int {
        for (line in header.split(CRLF)) {
            if (line.startsWith("Content-Length:", ignoreCase = true)) {
                return line.substringAfter(":").trim().toIntOrNull() ?: 0
            }
        }
        return 0
    }

    private fun newClient() = NewsEmbeddingClient(
        baseUrl = "http://127.0.0.1:" + serverSocket.localPort,
        connectTimeoutMs = CONNECT_TIMEOUT_MS,
        readTimeoutMs = READ_TIMEOUT_MS,
    )

    private val item = NewsEmbeddingItem(
        id = 1L,
        title = "제목",
        content = "본문",
        url = "https://example.com/1",
        stockId = 10L,
    )

    @Test
    fun `h2c 업그레이드 헤더 없이 HTTP_1_1로 보낸다`() {
        val response = newClient().ingest(listOf(item))

        assertTrue(requestReceived.await(10, TimeUnit.SECONDS), "요청이 서버에 도달하지 않았다")

        // JDK HttpClient 기본값(HTTP/2)으로 두면 평문 http에서 아래 헤더가 붙는다. 그러면 임베딩 서비스의
        // uvicorn(h11)이 프로토콜 전환 가능성 때문에 바디를 넘기지 않아 422(loc=["body"])가 난다.
        assertFalse(receivedRequest.contains("Upgrade: h2c"), "h2c 업그레이드 헤더가 붙었다: " + receivedRequest)
        assertFalse(receivedRequest.contains("HTTP2-Settings"), "HTTP2-Settings 헤더가 붙었다: " + receivedRequest)
        assertTrue(receivedRequest.startsWith("POST /v1/news HTTP/1.1"), "요청 라인이 예상과 다르다: " + receivedRequest)

        assertEquals(1, response?.embedded)
    }

    @Test
    fun `요청 바디에 뉴스 본문과 stock_id가 담긴다`() {
        newClient().ingest(listOf(item))

        assertTrue(requestReceived.await(10, TimeUnit.SECONDS), "요청이 서버에 도달하지 않았다")

        val body = receivedRequest.substringAfter(HEADER_END)
        assertTrue(body.contains("\"stock_id\":10"), "stock_id가 snake_case로 직렬화되지 않았다: " + body)
        assertTrue(body.contains("\"content\":\"본문\""), "본문이 바디에 없다: " + body)
    }
}
