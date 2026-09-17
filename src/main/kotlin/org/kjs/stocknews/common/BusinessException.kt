package org.kjs.stocknews.common

open class BusinessException(
    val resultCode: ResultCode,
    message: String = resultCode.message,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
