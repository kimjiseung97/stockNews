package org.kjs.stocknews.common

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ApiResponse<Nothing> =
        ApiResponse.fail(e.resultCode)

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
