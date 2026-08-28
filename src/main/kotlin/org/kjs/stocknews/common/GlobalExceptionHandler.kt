package org.kjs.stocknews.common

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ApiResponse<Nothing> {
        // 원인이 있는 경우(하위 계층 예외를 감싼 경우)만 로깅한다 - 입력 검증 실패 같은 일반적인
        // 비즈니스 실패는 원인이 없어 로그 잡음을 만들지 않는다.
        e.cause?.let { log.warn("business exception: {}", e.resultCode, it) }
        return ApiResponse.fail(e.resultCode)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ApiResponse<Nothing> =
        ApiResponse.fail(ResultCode.NOT_FOUND)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ApiResponse<Nothing> =
        ApiResponse.fail(ResultCode.BAD_REQUEST)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ApiResponse<Nothing> {
        log.error("Unhandled exception", e)
        return ApiResponse.fail(ResultCode.INTERNAL_ERROR)
    }
}
