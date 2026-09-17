package org.kjs.stocknews.config.ratelimit

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.assertEquals

/**
 * 요청 제한의 키를 정하는 부분이라, 여기가 뚫리면 제한 전체가 무의미해진다.
 * 특히 "X-Forwarded-For의 앞쪽 값을 믿으면 안 된다"는 회귀 방지가 이 테스트의 핵심이다.
 */
class ClientIpResolverTest {

    private fun resolver(trustedProxyCount: Int) =
        ClientIpResolver(RateLimitProperties().apply { this.trustedProxyCount = trustedProxyCount })

    private fun request(remoteAddr: String, forwardedFor: String? = null) =
        MockHttpServletRequest().apply {
            this.remoteAddr = remoteAddr
            forwardedFor?.let { addHeader("X-Forwarded-For", it) }
        }

    @Test
    fun `프록시가 없으면 X-Forwarded-For를 무시하고 실제 접속 IP를 쓴다`() {
        // 프록시가 없는데 헤더를 믿으면 누구나 헤더만 붙여 제한을 우회할 수 있다.
        val resolved = resolver(trustedProxyCount = 0)
            .resolve(request(remoteAddr = "5.5.5.5", forwardedFor = "1.2.3.4"))

        assertEquals("5.5.5.5", resolved)
    }

    @Test
    fun `프록시 한 대 뒤에서는 프록시가 덧붙인 마지막 값을 쓴다`() {
        val resolved = resolver(trustedProxyCount = 1)
            .resolve(request(remoteAddr = "172.18.0.2", forwardedFor = "203.0.113.7"))

        assertEquals("203.0.113.7", resolved)
    }

    @Test
    fun `클라이언트가 X-Forwarded-For를 위조해도 실제 IP로 집계된다`() {
        // 공격자가 "X-Forwarded-For: 1.2.3.4"를 붙이면 Caddy가 뒤에 실제 IP를 이어붙여
        // "1.2.3.4, 203.0.113.7"이 도착한다. 앞의 위조값을 믿으면 매 요청 키가 달라져 제한이 무력화된다.
        val resolved = resolver(trustedProxyCount = 1)
            .resolve(request(remoteAddr = "172.18.0.2", forwardedFor = "1.2.3.4, 203.0.113.7"))

        assertEquals("203.0.113.7", resolved)
    }

    @Test
    fun `위조값을 여러 개 붙여도 실제 IP로 집계된다`() {
        val resolved = resolver(trustedProxyCount = 1)
            .resolve(request(remoteAddr = "172.18.0.2", forwardedFor = "1.1.1.1, 2.2.2.2, 3.3.3.3, 203.0.113.7"))

        assertEquals("203.0.113.7", resolved)
    }

    @Test
    fun `프록시를 거치지 않고 직접 들어온 요청은 헤더를 믿지 않는다`() {
        // 홉 수가 기대(1)보다 적으면 프록시를 우회한 요청이므로 TCP 상대 주소로 떨어진다.
        val resolver = ClientIpResolver(RateLimitProperties().apply { trustedProxyCount = 2 })

        val resolved = resolver.resolve(request(remoteAddr = "172.18.0.9", forwardedFor = "1.2.3.4"))

        assertEquals("172.18.0.9", resolved)
    }

    @Test
    fun `X-Forwarded-For가 없으면 실제 접속 IP를 쓴다`() {
        val resolved = resolver(trustedProxyCount = 1).resolve(request(remoteAddr = "172.18.0.2"))

        assertEquals("172.18.0.2", resolved)
    }
}
