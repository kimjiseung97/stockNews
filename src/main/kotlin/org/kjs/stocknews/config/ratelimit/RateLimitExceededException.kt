package org.kjs.stocknews.config.ratelimit

import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode

/**
 * IP 단위 요청 한도를 넘겨 컨트롤러 진입 전에 거절된 요청.
 *
 * [BusinessException]을 상속한 이유: 전용 핸들러(429)를 실수로 지워도 기존
 * BusinessException 핸들러가 받아 TOO_MANY_REQUESTS 응답은 그대로 나가게 하려는 것.
 *
 * @property retryAfterSeconds 다시 시도 가능해질 때까지 남은 초(Retry-After 헤더 값)
 */
class RateLimitExceededException(
    val retryAfterSeconds: Long,
) : BusinessException(ResultCode.TOO_MANY_REQUESTS)
