package org.kjs.stocknews.config.ratelimit

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

/**
 * IP 단위 요청 제한 설정.
 *
 * 임계치를 코드가 아닌 설정으로 둔 이유: 운영에서 실제 트래픽을 보고 조정해야 하는 값이고,
 * 오탐(정상 사용자가 막힘)이 발생하면 재배포 없이 env로 바로 낮출 수 있어야 한다.
 *
 * 값 검증을 붙인 이유: 잘못된 값은 기동은 성공시키고 첫 요청에서야 터지거나(버킷 생성 실패),
 * 조용히 제한을 무력화한다(매 요청 새 버킷). 기동 시점에 실패하는 편이 낫다.
 */
@Validated
@ConfigurationProperties(prefix = "rate-limit")
class RateLimitProperties {
    /** 끄고 켤 수 있어야 한다 - 오탐으로 서비스가 막혔을 때 즉시 무력화할 수 있는 스위치. */
    var enabled: Boolean = true

    /** [refillPeriod] 동안 한 IP가 쓸 수 있는 최대 요청 수(= 버스트 허용치). */
    @field:Positive
    var capacity: Long = 120

    /** [capacity]만큼의 토큰이 다시 차는 데 걸리는 시간. */
    var refillPeriod: Duration = Duration.ofMinutes(1)

    /**
     * 동시에 추적할 IP 개수 상한.
     * IP는 사실상 무한히 다양해서 상한이 없으면 공격 자체가 메모리 고갈 수단이 된다.
     */
    @field:Positive
    var maxTrackedClients: Long = 100_000

    /** 이 시간 동안 요청이 없는 IP의 버킷은 버린다(= 다음 요청 때 새 버킷으로 시작). */
    var clientIdleTimeout: Duration = Duration.ofMinutes(10)

    /**
     * 앱 앞에 있는 신뢰할 수 있는 프록시 수. X-Forwarded-For를 뒤에서부터 몇 번째까지 믿을지를 정한다.
     * 자세한 이유는 [ClientIpResolver] 참고. 프록시 없이 직접 노출되는 환경은 0이어야 한다.
     */
    @field:PositiveOrZero
    var trustedProxyCount: Int = 0

    /**
     * 거절 로그를 분당 최대 몇 줄까지 남길지.
     * 거절 1건마다 로그를 남기면 공격 트래픽이 곧 로그 폭주가 되어(json-file 10MB x 3)
     * 로테이션으로 다른 로그를 전부 밀어낸다 - 공격자가 흔적을 지우는 수단이 된다.
     */
    @field:Positive
    var logLinesPerMinute: Long = 20

    @AssertTrue(message = "rate-limit.refill-period must be positive")
    fun isRefillPeriodValid(): Boolean = !refillPeriod.isZero && !refillPeriod.isNegative

    @AssertTrue(message = "rate-limit.client-idle-timeout must be positive")
    fun isClientIdleTimeoutValid(): Boolean = !clientIdleTimeout.isZero && !clientIdleTimeout.isNegative
}
