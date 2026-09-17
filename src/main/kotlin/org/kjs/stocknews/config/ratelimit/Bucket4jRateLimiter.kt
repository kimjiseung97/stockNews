package org.kjs.stocknews.config.ratelimit

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Caffeine에 IP별 토큰 버킷을 담아두는 인메모리 [RateLimiter].
 *
 * 고정 윈도우 카운터(N초마다 0으로 리셋) 대신 토큰 버킷을 쓴 이유: 고정 윈도우는 윈도우 경계에
 * 요청을 몰면 짧은 순간 한도의 2배가 통과한다. 토큰 버킷은 토큰이 시간에 비례해 조금씩 차오르므로
 * 그 구멍이 없다.
 *
 * 버킷 보관에 평범한 Map을 쓰지 않은 이유: IP는 무한히 다양해서 만료·개수 제한이 없으면
 * 요청을 뿌리는 것만으로 힙이 계속 늘어난다. Caffeine이 유휴 만료와 개수 상한을 대신 처리한다.
 */
@Component
class Bucket4jRateLimiter(
    private val properties: RateLimitProperties,
) : RateLimiter {

    private val buckets = Caffeine.newBuilder()
        .maximumSize(properties.maxTrackedClients)
        .expireAfterAccess(properties.clientIdleTimeout)
        .build<String, Bucket> { newBucket() }

    override fun tryConsume(key: String): RateLimitVerdict {
        val probe = buckets.get(key).tryConsumeAndReturnRemaining(1)
        return RateLimitVerdict(
            allowed = probe.isConsumed,
            remainingTokens = probe.remainingTokens,
            retryAfter = Duration.ofNanos(probe.nanosToWaitForRefill),
        )
    }

    private fun newBucket(): Bucket =
        Bucket.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(properties.capacity)
                    .refillGreedy(properties.capacity, properties.refillPeriod)
                    .build(),
            )
            .build()
}
