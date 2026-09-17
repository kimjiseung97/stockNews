package org.kjs.stocknews.config.ratelimit

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 토큰 버킷 자체의 동작 검증.
 * 스프링 컨텍스트 없이 도는 순수 단위 테스트라, 한도/키 분리 같은 핵심 규칙이 깨지면 여기서 먼저 잡힌다.
 */
class Bucket4jRateLimiterTest {

    private fun limiter(capacity: Long, refillPeriod: Duration = Duration.ofMinutes(1)) =
        Bucket4jRateLimiter(
            RateLimitProperties().apply {
                this.capacity = capacity
                this.refillPeriod = refillPeriod
            },
        )

    @Test
    fun `한도까지는 허용하고 한도를 넘으면 거절한다`() {
        val rateLimiter = limiter(capacity = 3)

        repeat(3) { assertTrue(rateLimiter.tryConsume("1.1.1.1").allowed, "${it + 1}번째 요청은 허용돼야 한다") }

        assertFalse(rateLimiter.tryConsume("1.1.1.1").allowed)
    }

    @Test
    fun `IP가 다르면 서로의 한도에 영향을 주지 않는다`() {
        val rateLimiter = limiter(capacity = 1)

        assertTrue(rateLimiter.tryConsume("1.1.1.1").allowed)
        assertFalse(rateLimiter.tryConsume("1.1.1.1").allowed)

        // 한 IP가 한도를 소진해도 다른 IP는 정상이어야 한다 - 이게 깨지면 전 사용자가 같이 막힌다.
        assertTrue(rateLimiter.tryConsume("2.2.2.2").allowed)
    }

    @Test
    fun `거절된 요청은 토큰을 소비하지 않는다`() {
        val rateLimiter = limiter(capacity = 1)

        rateLimiter.tryConsume("1.1.1.1")
        repeat(5) { rateLimiter.tryConsume("1.1.1.1") }

        // 거절이 토큰을 깎으면 잔량이 음수로 내려가 재시도 대기시간이 실제보다 길어진다.
        assertEquals(0, rateLimiter.tryConsume("1.1.1.1").remainingTokens)
    }

    @Test
    fun `거절 시 재시도 대기시간을 알려준다`() {
        val rateLimiter = limiter(capacity = 1, refillPeriod = Duration.ofMinutes(1))

        rateLimiter.tryConsume("1.1.1.1")
        val verdict = rateLimiter.tryConsume("1.1.1.1")

        assertFalse(verdict.allowed)
        assertTrue(verdict.retryAfter > Duration.ZERO, "Retry-After에 내려줄 대기시간이 있어야 한다")
        assertTrue(verdict.retryAfter <= Duration.ofMinutes(1))
    }

    @Test
    fun `토큰은 시간이 지나면 조금씩 다시 찬다`() {
        // 고정 윈도우와 달리 토큰 버킷은 주기가 끝나기 전에도 일부가 회복된다.
        val rateLimiter = limiter(capacity = 10, refillPeriod = Duration.ofMillis(200))
        repeat(10) { rateLimiter.tryConsume("1.1.1.1") }
        assertFalse(rateLimiter.tryConsume("1.1.1.1").allowed)

        Thread.sleep(250)

        assertTrue(rateLimiter.tryConsume("1.1.1.1").allowed)
    }
}
