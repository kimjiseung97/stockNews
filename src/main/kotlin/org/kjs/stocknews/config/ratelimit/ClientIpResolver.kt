package org.kjs.stocknews.config.ratelimit

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

private const val X_FORWARDED_FOR = "X-Forwarded-For"

/**
 * 요청 제한 키로 쓸 클라이언트 IP를 구한다.
 *
 * `server.forward-headers-strategy`(ForwardedHeaderFilter)를 쓰지 않는 이유가 여기 있다.
 * 그 필터는 X-Forwarded-For의 **첫 번째** 값을 클라이언트 IP로 삼는데, Caddy를 비롯한 프록시는
 * 들어온 헤더를 지우지 않고 뒤에 이어붙인다. 따라서 공격자가 `X-Forwarded-For: 1.2.3.4`를 붙여
 * 보내면 앱에는 `1.2.3.4, <공격자 실제 IP>`가 도착하고, 필터는 앞의 위조값을 믿는다.
 * 매 요청마다 값을 바꾸면 버킷도 매번 새로 생겨 제한이 통째로 무력화된다.
 *
 * 그래서 앞이 아니라 **뒤에서부터** 센다. 우리 프록시가 마지막에 덧붙인 값은 프록시가 실제로 본
 * TCP 상대 주소라 위조할 수 없다. [RateLimitProperties.trustedProxyCount]가 우리 앞에 몇 대의
 * 프록시가 있는지(= 뒤에서 몇 번째가 신뢰할 값인지)를 가리킨다.
 *
 * 프록시가 없는 환경(기본값 0)에서는 X-Forwarded-For를 아예 쳐다보지 않는다. 헤더는 누구나 붙일 수
 * 있어서, 프록시가 없는데 믿으면 그 자체가 우회 수단이 된다.
 */
@Component
class ClientIpResolver(
    private val properties: RateLimitProperties,
) {
    fun resolve(request: HttpServletRequest): String? {
        val trustedProxyCount = properties.trustedProxyCount

        // 프록시가 없는 환경에서는 헤더를 보지 않고 TCP 상대 주소만 쓴다.
        if (trustedProxyCount <= 0) {
            return request.remoteAddr
        }

        val forwardedFor = request.getHeader(X_FORWARDED_FOR)
        if (forwardedFor == null) {
            return request.remoteAddr
        }

        val hops = forwardedFor.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val trustedIndex = hops.size - trustedProxyCount

        // 홉 수가 기대보다 적으면(= 프록시를 거치지 않고 직접 들어온 요청) 헤더를 믿지 않는다.
        if (trustedIndex < 0) {
            return request.remoteAddr
        }

        return hops[trustedIndex]
    }
}
