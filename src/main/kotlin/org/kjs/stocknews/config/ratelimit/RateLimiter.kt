package org.kjs.stocknews.config.ratelimit

import java.time.Duration

/**
 * 요청 허용 여부를 판단하는 카운터.
 *
 * 구현을 인터페이스 뒤에 둔 이유: 지금은 앱 인스턴스가 1대라 인메모리([Bucket4jRateLimiter])로 충분하지만,
 * 인스턴스를 2대 이상으로 늘리면 카운터를 공유해야 해서 Redis 구현으로 갈아끼워야 한다.
 * 그때 인터셉터와 설정은 그대로 두고 이 구현만 교체하면 된다.
 */
interface RateLimiter {
    /**
     * [key]의 토큰 하나를 소비한다.
     * 남은 토큰이 없으면 소비하지 않고 거절 결과를 돌려준다.
     */
    fun tryConsume(key: String): RateLimitVerdict
}

/**
 * @property allowed 요청을 통과시켜도 되는지
 * @property remainingTokens 이번 소비 후 남은 요청 가능 횟수
 * @property retryAfter 거절된 경우 다시 시도 가능해질 때까지의 시간(허용된 경우 [Duration.ZERO])
 */
data class RateLimitVerdict(
    val allowed: Boolean,
    val remainingTokens: Long,
    val retryAfter: Duration,
)
