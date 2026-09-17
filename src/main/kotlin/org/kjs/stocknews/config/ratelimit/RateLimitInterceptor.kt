package org.kjs.stocknews.config.ratelimit

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsUtils
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Duration

/**
 * 같은 IP에서 짧은 시간에 몰려오는 요청을 컨트롤러 진입 전에 잘라낸다.
 *
 * Filter가 아니라 HandlerInterceptor로 구현한 이유: Filter에서 예외를 던지면 DispatcherServlet
 * 바깥이라 [org.kjs.stocknews.common.GlobalExceptionHandler]가 받지 못해 거절 응답만
 * ApiResponse 포맷을 벗어난다. 인터셉터에서 던지면 기존 예외 처리 경로를 그대로 탄다.
 * (대신 핸들러 매핑까지는 진행된 뒤 잘린다. 그보다 앞단의 대량 트래픽 차단은 리버스 프록시 몫이다.)
 *
 * 클라이언트 IP 판별은 [ClientIpResolver]가 담당한다 - 위조 가능한 헤더를 어디까지 믿을지가
 * 이 기능의 성패를 가르므로 별도 클래스로 분리해 두었다.
 */
@Component
class RateLimitInterceptor(
    private val rateLimiter: RateLimiter,
    private val clientIpResolver: ClientIpResolver,
    private val properties: RateLimitProperties,
) : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(RateLimitInterceptor::class.java)

    /**
     * 거절 로그 예산. IP별로 한 줄씩만 남겨도 공격자가 IP를 바꿔가며 쏘면 로그는 그대로 폭주하므로,
     * IP와 무관한 전역 한도를 둔다. 초과분은 버리고 개별 IP는 로그 대신 429 응답으로만 남는다.
     */
    private val logBudget: Bucket = Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(properties.logLinesPerMinute)
                .refillGreedy(properties.logLinesPerMinute, Duration.ofMinutes(1))
                .build(),
        )
        .build()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (!properties.enabled) {
            return true
        }

        // CORS preflight(OPTIONS)는 세지 않는다. 세면 POST 한 번이 토큰 2개를 먹고,
        // preflight가 막히는 순간 브라우저에는 원인을 알 수 없는 CORS 오류로 보인다.
        // preflight 자체는 컨트롤러/DB를 타지 않아 흘려보내도 비용이 거의 없다.
        if (CorsUtils.isPreFlightRequest(request)) {
            return true
        }

        // IP를 못 구하면 셀 기준이 없으므로 막지 않고 통과시킨다.
        val clientIp = clientIpResolver.resolve(request)
        if (clientIp == null) {
            return true
        }

        val verdict = rateLimiter.tryConsume(clientIp)
        if (verdict.allowed) {
            return true
        }

        if (logBudget.tryConsume(1)) {
            log.warn("rate limit exceeded: ip={} method={} path={}", clientIp, request.method, request.requestURI)
        }

        // 올림 처리한다 - 내림하면 안내한 시간에 재시도해도 아직 토큰이 없어 다시 429를 받는다.
        val retryAfterSeconds = ceilToSeconds(verdict.retryAfter)
        throw RateLimitExceededException(retryAfterSeconds)
    }

    private fun ceilToSeconds(duration: Duration): Long {
        val nanos = duration.toNanos().coerceAtLeast(1)
        return (nanos + 999_999_999) / 1_000_000_000
    }
}
