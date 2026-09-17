package org.kjs.stocknews.config.ratelimit

import org.junit.jupiter.api.Test
import org.kjs.stocknews.common.GlobalExceptionHandler
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.test.assertTrue

/**
 * 요청 제한이 실제 MVC 경로에서 어떤 응답으로 나가는지 검증한다.
 *
 * 전체 컨텍스트(@SpringBootTest) 대신 standalone MockMvc를 쓰는 이유: 이 앱은 애플리케이션 클래스에
 * @EnableBatchProcessing이 붙어 있어 어떤 슬라이스로 띄워도 jobRepository -> DataSource가 필요하다.
 * 여기서 확인하려는 건 거절 응답의 형태(429 / TOO_MANY_REQUESTS / Retry-After)와 IP 분리이지 DB가
 * 아니므로, 인터셉터와 예외 처리기만 실제 객체로 엮어 검증한다.
 * 어떤 경로에 인터셉터가 붙는지(WebConfig 등록부)는 AuthInterceptorTest가 전체 컨텍스트로 확인한다.
 */
class RateLimitInterceptorTest {

    /** 인터셉터만 보려는 테스트라 실제 컨트롤러 대신 의존성 없는 최소 핸들러를 쓴다. */
    @RestController
    @RequestMapping("/stocks")
    class ProbeController {
        @GetMapping
        fun probe(): String = "ok"
    }

    private fun mockMvc(properties: RateLimitProperties): MockMvc =
        MockMvcBuilders.standaloneSetup(ProbeController())
            .addMappedInterceptors(arrayOf("/stocks/**"), RateLimitInterceptor(Bucket4jRateLimiter(properties), ClientIpResolver(properties), properties))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()

    private fun properties(capacity: Long, enabled: Boolean = true) =
        RateLimitProperties().apply {
            this.enabled = enabled
            this.capacity = capacity
        }

    private fun MockHttpServletRequestBuilder.from(ip: String) = this.with { it.remoteAddr = ip; it }

    @Test
    fun `한도를 넘기면 429와 TOO_MANY_REQUESTS를 내려준다`() {
        val mvc = mockMvc(properties(capacity = 3))

        repeat(3) { mvc.perform(get("/stocks").from("10.0.0.1")).andExpect(status().isOk) }

        mvc.perform(get("/stocks").from("10.0.0.1"))
            .andExpect(status().isTooManyRequests)
            .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
            .andExpect(header().exists("Retry-After"))
    }

    @Test
    fun `한 IP가 한도를 소진해도 다른 IP는 정상 처리된다`() {
        val mvc = mockMvc(properties(capacity = 1))

        repeat(3) { mvc.perform(get("/stocks").from("10.0.0.2")) }

        // 이게 깨지면 한 명의 과요청으로 전 사용자가 같이 막힌다.
        mvc.perform(get("/stocks").from("10.0.0.3")).andExpect(status().isOk)
    }

    @Test
    fun `CORS preflight는 요청 수로 세지 않는다`() {
        // 운영에서는 preflight(OPTIONS)도 인터셉터 체인을 탄다. 그대로 세면 POST 한 번이 토큰 2개를
        // 먹고, preflight가 막히는 순간 브라우저에는 원인을 알 수 없는 CORS 오류로만 보인다.
        // (MockMvc standalone에는 preflight용 HandlerAdapter가 없어 인터셉터를 직접 호출해 확인한다.)
        val properties = properties(capacity = 2)
        val interceptor = RateLimitInterceptor(Bucket4jRateLimiter(properties), ClientIpResolver(properties), properties)
        val response = MockHttpServletResponse()

        repeat(5) {
            val preflight = MockHttpServletRequest("OPTIONS", "/stocks").apply {
                remoteAddr = "10.0.0.4"
                addHeader("Origin", "http://localhost:5173")
                addHeader("Access-Control-Request-Method", "GET")
            }
            assertTrue(interceptor.preHandle(preflight, response, Any()))
        }

        // preflight를 세지 않았다면 실제 요청용 토큰 2개가 그대로 남아 있어야 한다.
        repeat(2) {
            val actual = MockHttpServletRequest("GET", "/stocks").apply { remoteAddr = "10.0.0.4" }
            assertTrue(interceptor.preHandle(actual, response, Any()))
        }
    }

    @Test
    fun `요청 제한을 끄면 한도를 넘겨도 통과한다`() {
        // 오탐으로 서비스가 막혔을 때 재배포 없이 끌 수 있어야 한다 - 이 스위치가 죽으면 대응 수단이 없다.
        val disabled = properties(capacity = 1, enabled = false)
        val interceptor = RateLimitInterceptor(Bucket4jRateLimiter(disabled), ClientIpResolver(disabled), disabled)
        val response = MockHttpServletResponse()

        repeat(10) {
            val request = MockHttpServletRequest("GET", "/stocks").apply { remoteAddr = "10.0.0.5" }
            assertTrue(interceptor.preHandle(request, response, Any()))
        }
    }
}
